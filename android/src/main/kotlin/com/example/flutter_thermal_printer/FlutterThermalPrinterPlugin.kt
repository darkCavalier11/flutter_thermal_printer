package com.example.flutter_thermal_printer


import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.getSystemService
import com.example.flutter_thermal_printer.models.BluetoothPrinter
import com.example.flutter_thermal_printer.models.OfflineOrderLabel
import com.example.flutter_thermal_printer.models.PrintableReceipt
import com.google.gson.Gson
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import net.posprinter.posprinterface.IMyBinder
import net.posprinter.posprinterface.ProcessData
import net.posprinter.posprinterface.TaskCallback
import net.posprinter.service.PosprinterService
import net.posprinter.utils.BitmapProcess
import net.posprinter.utils.BitmapToByteData
import net.posprinter.utils.DataForSendToPrinterPos58
import net.posprinter.utils.DataForSendToPrinterTSC
import kotlin.math.log


/** FlutterThermalPrinterPlugin */
class FlutterThermalPrinterPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  private var activity: Activity? = null
  private lateinit var context: Context
  private var bluetoothAdapter: BluetoothAdapter? = null
  private var bluetoothManager: BluetoothManager? = null

  private var bluetoothPrintBinder: IMyBinder? = null
  private var thermalPrinterDevices = mutableSetOf<BluetoothDevice>()
  private var connectedThermalPrinter: BluetoothDevice? = null
  private var bluetoothServiceConnection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
      bluetoothPrintBinder = service as IMyBinder
      logger("onServiceConnected(name: ComponentName, service: IBinder)")
    }

    override fun onServiceDisconnected(name: ComponentName) {
      logger("onServiceDisconnected(name: ComponentName)")
    }
  }
  private fun logger(text: Any) {
    Log.d("ThermalPrinter", "$text")
  }

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_thermal_printer")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    //bind service，get imyBinder
    val intent: Intent = Intent(context, PosprinterService::class.java)
    context.bindService(intent, bluetoothServiceConnection, Context.BIND_AUTO_CREATE)
  }

  @RequiresApi(Build.VERSION_CODES.S)
  private fun initialise() {
    if (bluetoothAdapter != null) {
      return
    }
    logger("Asking bluetooth permissions")
    requestBluetoothPermission()
    logger("Initialising Bluetooth manager and adapter")
    bluetoothManager = getSystemService(context, BluetoothManager::class.java)
    bluetoothAdapter = bluetoothManager?.adapter
    if (bluetoothAdapter == null) {
      Toast.makeText(context, "Bluetooth adapter not found", Toast.LENGTH_SHORT).show()
    } else if (!bluetoothAdapter!!.isEnabled) {
      logger("Enabling bluetooth for connection with printer")
      val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      startActivityForResult(activity!!,  enableBtIntent, 1, null)
    }
  }

  @RequiresApi(Build.VERSION_CODES.S)
  private fun requestBluetoothPermission() {
    if (ActivityCompat.checkSelfPermission(
        context,
        android.Manifest.permission.BLUETOOTH_SCAN
      ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
        context,
        android.Manifest.permission.BLUETOOTH_CONNECT
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      ActivityCompat.requestPermissions(
        activity!!,
        arrayOf<String>(
          android.Manifest.permission.BLUETOOTH_SCAN,
          android.Manifest.permission.BLUETOOTH_CONNECT
        ),
        1024
      )
    }
  }

  @RequiresApi(Build.VERSION_CODES.S)
  private fun getAllBluetoothPairedDevices(call: MethodCall, result: Result) {
    if (bluetoothAdapter == null || bluetoothManager == null) {
      return
    }
    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
      requestBluetoothPermission()
      return
    }
    if (!bluetoothAdapter!!.isDiscovering) {
      bluetoothAdapter!!.startDiscovery()
    }
    val allPairedDevices = bluetoothAdapter!!.bondedDevices
    logger("all available paired devices: $allPairedDevices")
    val bluetoothPrintersMap = mutableListOf<Map<String, Any>>()
    for (device in allPairedDevices) {
      val majorDeviceClass: Int = device.bluetoothClass.majorDeviceClass
      val deviceClass: Int = device.bluetoothClass.deviceClass
      logger("Device details Maj: $majorDeviceClass, Dev: $deviceClass")
      if (majorDeviceClass == 1536 && (deviceClass == 1664 || deviceClass == 1536)) {
        thermalPrinterDevices.add(device)
        bluetoothPrintersMap.add(BluetoothPrinter(device.address, device.name).toJson())
      }
    }
    logger("all thermal printers found $thermalPrinterDevices")
    result.success(bluetoothPrintersMap)
  }

  private fun connectToBluetoothPrinterByAddress(call: MethodCall, result: Result) {
    val address = call.argument<String>("bluetooth_printer_address")
    if (connectedThermalPrinter?.address == address) {
      return;
    }

    try {
      val selectedPrinter = thermalPrinterDevices.first { bluetoothDevice ->  bluetoothDevice.address == address }
      logger("Found printer by address $address, trying to connect")
      if (bluetoothAdapter != null && bluetoothAdapter!!.isDiscovering) {
        bluetoothAdapter!!.cancelDiscovery()
      }
      if (bluetoothPrintBinder == null) {
        logger("myBinder is null, connection failed")
      }
      bluetoothPrintBinder!!.ConnectBtPort(address, object : TaskCallback {
        override fun OnSucceed() {
          logger("Connection successful TaskCallback")
          connectedThermalPrinter = selectedPrinter
          result.success(true)
        }

        override fun OnFailed() {
          logger("Connection failed onFailed() TaskCallback")
          connectedThermalPrinter = null
          result.success(false)
        }
      })
    } catch (error: Error) {
      logger("Error connecting printer to address $address: $error")
      result.error(
        "NOT FOUND",
        "Unable to connect to the printer with $address",
        "Error occurred while connecting to the printer with address $address. Make sure printer is on, and paired with the device"
      )
      result.success(false)
    }

  }

  private fun isConnectedToBluetoothThermalPrinter(call: MethodCall, result: Result) {
    val address = call.argument<String>("bluetooth_printer_address")
    logger("Checking connection status with printer $address")
    if (connectedThermalPrinter == null) {
      return result.success(false)
    }
    result.success(connectedThermalPrinter?.address == address)
  }

  private fun disconnectBluetoothThermalPrinter(call: MethodCall, result: Result) {
    if (connectedThermalPrinter == null) {
      return
    }
    if (bluetoothPrintBinder == null) {
      logger("myBinder is null, disconnectBluetoothThermalPrinter()")
    }
    bluetoothPrintBinder!!.RemovePrinter(connectedThermalPrinter?.name, object : TaskCallback {
      override fun OnSucceed() {
        connectedThermalPrinter = null
        result.success(true)
      }
      override fun OnFailed() {
        result.success(false)
      }
    })
  }

  private fun printStringWithBluetoothPrinter(call: MethodCall, result: Result) {
    val s = call.argument<String>("printable_string") ?: return
    logger("called printStringWithBluetoothPrinter() with print ${connectedThermalPrinter?.address} with payload $s")
    if (connectedThermalPrinter != null) {
      bluetoothPrintBinder?.WriteSendData(object : TaskCallback {
        override fun OnSucceed() {
          logger("printStringWithBluetoothPrinter() successfully sent data for printing")
          result.success(true)
        }

        override fun OnFailed() {
          logger("printStringWithBluetoothPrinter() failed to send data for printing")
          result.success(false)
        }
      }, ProcessData {
        val list: MutableList<ByteArray> = java.util.ArrayList()
        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add(s.encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list
      })
      result.success(true)
    } else {
      result.error("NO PRINTER FOUND", "connect to printer before print", "Try to connect to printer before printing.")
    }
  }

  private fun printReceiptWithBluetoothPrinter(call: MethodCall, result: Result) {
    val printableReceiptMap = call.argument<Map<String, Any>>("printable_receipt")
    val qrCodeText = call.argument<String?>("qr_code_text")
    val gson = Gson()
    val printableReceipt = gson.fromJson(gson.toJson(printableReceiptMap), PrintableReceipt::class.java)
    logger("printReceiptWithBluetoothPrinter() with $printableReceiptMap, $qrCodeText with $connectedThermalPrinter")
    if (connectedThermalPrinter == null) {
      result.error("NO PRINTER FOUND", "connect to printer before print", "Try to connect to printer before printing.")
      return
    }
    bluetoothPrintBinder?.WriteSendData(object : TaskCallback {
      override fun OnSucceed() {
        logger("printStringWithBluetoothPrinter() successfully sent data for printing")
      }

      override fun OnFailed() {
        logger("printStringWithBluetoothPrinter() failed to send data for printing")
        result.success(false)
      }
    }, ProcessData {
      printableReceipt.generatePrintableByteArray(qrCodeText)
    })
  }
  fun bitmapFromArray(pixels2d: Array<IntArray>): Bitmap? {
    val width = pixels2d.size
    val height = pixels2d[0].size
    val pixels = IntArray(width * height)
    var pixelsIndex = 0
    for (i in 0 until width) {
      for (j in 0 until height) {
        pixels[pixelsIndex] = pixels2d[i][j]
        pixelsIndex++
      }
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
  }

  fun drawableToBitmap(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable) {
      return drawable.bitmap
    }

    // Create a Bitmap of the same size as the Drawable
    val width = drawable.intrinsicWidth
    val height = drawable.intrinsicHeight
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)

    // Create a Canvas to draw the Drawable onto the Bitmap
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
  }
  private fun printOfflineOrderLabel(call: MethodCall, result: Result) {
    val offlineOrderLabelMap = call.argument<Map<String, Any>>("offline_order_label")
    val gson = Gson()
    val offlineOrderLabel = gson.fromJson(gson.toJson(offlineOrderLabelMap), OfflineOrderLabel::class.java)
    logger(offlineOrderLabelMap!!)
    if (connectedThermalPrinter == null) {
      result.error("NO PRINTER FOUND", "connect to printer before print", "Try to connect to printer before printing.")
      return
    }
    bluetoothPrintBinder!!.WriteSendData(object : TaskCallback {
      override fun OnSucceed() {
      }

      override fun OnFailed() {
      }
    }, ProcessData {
//       width = 4.0 inch, height = 2.0 inch
      val width = 2.54 * 10 * 4.0
      val height = 2.54 * 10 * 2.0
      // padding of 50mm from all sides
      val padding = 50

      val list: MutableList<ByteArray> = ArrayList()
      val packageManager: PackageManager = context.packageManager
      val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
      val bitmap1 = BitmapProcess.compressBmpByYourWidth(
        BitmapFactory.decodeResource(
          context.resources,
          R.drawable.splash,
        ), 150
      )

      val logo = BitmapProcess.compressBmpByYourWidth(
        BitmapFactory.decodeResource(
          context.resources,
          R.drawable.app_icon,
        ), 150
      )

      list.add(DataForSendToPrinterTSC.sizeBymm(width, height))
      list.add(DataForSendToPrinterTSC.direction(0))
      list.add(DataForSendToPrinterTSC.cls())
      list.add(DataForSendToPrinterTSC.bitmap(550, 40, 0, bitmap1, BitmapToByteData.BmpType.Threshold))
      list.add(DataForSendToPrinterTSC.qrCode(110, 100, "M", 5, "A", 0, "M1", "S3", offlineOrderLabel.qrCodeText))
      list.add(DataForSendToPrinterTSC.text(150, 25, "4", 0, 1, 2, offlineOrderLabel.businessName))
      list.add(DataForSendToPrinterTSC.text(400, 120, "monospace", 0, 1, 1, offlineOrderLabel.customerName))
      list.add(DataForSendToPrinterTSC.text(400, 160, "monospace", 0, 1, 1, "Mob: " + offlineOrderLabel.customerPhone))
      list.add(DataForSendToPrinterTSC.text(400, 200, "monospace", 0, 1, 1, "Credit Issued: " + offlineOrderLabel.creditIssued))
      list.add(DataForSendToPrinterTSC.text(400, 240, "monospace", 0, 1, 1, "Issued on: " + offlineOrderLabel.issuedOn))
      list.add(DataForSendToPrinterTSC.text(400, 280, "monospace", 0, 1, 1, "valid till: " + offlineOrderLabel.validTill))
      list.add(DataForSendToPrinterTSC.text(150, 350, "monospace", 0, 1, 1, offlineOrderLabel.tokenId))

      list.add(DataForSendToPrinterTSC.print(1, 1))
      list
    })



  }




  @RequiresApi(Build.VERSION_CODES.S)
  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
      "initialise" -> initialise()
      "getAllBluetoothPairedDevices" -> getAllBluetoothPairedDevices(call, result)
      "connectToBluetoothPrinterByAddress" -> connectToBluetoothPrinterByAddress(call, result)
      "isConnectedToBluetoothThermalPrinter" -> isConnectedToBluetoothThermalPrinter(call, result)
      "disconnectBluetoothThermalPrinter" -> disconnectBluetoothThermalPrinter(call, result)
      "printStringWithBluetoothPrinter" -> printStringWithBluetoothPrinter(call, result)
      "printReceiptWithBluetoothPrinter" -> printReceiptWithBluetoothPrinter(call, result)
      "printOfflineOrderLabel" -> printOfflineOrderLabel(call, result)
      else -> result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivity() {
    activity = null
  }
}


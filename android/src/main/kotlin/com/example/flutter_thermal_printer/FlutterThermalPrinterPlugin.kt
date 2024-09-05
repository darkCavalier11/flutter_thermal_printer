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
import com.example.flutter_thermal_printer.models.PrintableReceipt
import com.google.gson.Gson
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.encoder.QRCode
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
import net.posprinter.utils.DataForSendToPrinterPos58
import java.util.EnumMap
import kotlin.math.ceil
import kotlin.math.roundToInt

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

  private var myBinder: IMyBinder? = null
  private var thermalPrinterDevices = ArrayList<BluetoothDevice>()
  private var connectedThermalPrinter: BluetoothDevice? = null
  private var mSerconnection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
      myBinder = service as IMyBinder
      logger("onServiceConnected(name: ComponentName, service: IBinder)")
    }

    override fun onServiceDisconnected(name: ComponentName) {
      logger("onServiceDisconnected(name: ComponentName)")
    }
  }
  private fun logger(text: Any) {
    Log.d("ThermalPrinter", "$text")
  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_thermal_printer")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    //bind service，get imyBinder
    val intent: Intent = Intent(context, PosprinterService::class.java)
    context.bindService(intent, mSerconnection, Context.BIND_AUTO_CREATE)
  }

  @RequiresApi(Build.VERSION_CODES.S)
  private fun initialise() {
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
      if (myBinder == null) {
        logger("myBinder is null, connection failed")
      }
      myBinder!!.ConnectBtPort(address, object : TaskCallback {
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
    if (myBinder == null) {
      logger("myBinder is null, disconnectBluetoothThermalPrinter()")
    }
    myBinder!!.RemovePrinter(connectedThermalPrinter?.name, object : TaskCallback {
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
      myBinder?.WriteSendData(object : TaskCallback {
        override fun OnSucceed() {
          logger("printStringWithBluetoothPrinter() successfully sent data for printing")
        }

        override fun OnFailed() {
          logger("printStringWithBluetoothPrinter() failed to send data for printing")
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
    myBinder?.WriteSendData(object : TaskCallback {
      override fun OnSucceed() {
        logger("printStringWithBluetoothPrinter() successfully sent data for printing")
      }

      override fun OnFailed() {
        logger("printStringWithBluetoothPrinter() failed to send data for printing")
      }
    }, ProcessData {
      val list: MutableList<ByteArray> = java.util.ArrayList()
//      list.add(DataForSendToPrinterPos58.initializePrinter())
//      list.add(DataForSendToPrinterPos58.selectAlignment(1))
//      list.add(DataForSendToPrinterPos58.selectCharacterSize(18))
//      list.add(printableReceipt.orderId.encodeToByteArray())
//      list.add(DataForSendToPrinterPos58.printAndFeedLine())
//
//      list.add(DataForSendToPrinterPos58.selectCharacterSize(16))
//      list.add(printableReceipt.datetime.encodeToByteArray())
//      list.add(DataForSendToPrinterPos58.printAndFeedLine())
//
//      list.add(printableReceipt.businessName.encodeToByteArray())
//      list.add(DataForSendToPrinterPos58.printAndFeedLine())
//      list.add(DataForSendToPrinterPos58.selectOrCancelBoldModel(1))
//
//      list.add("Customer Ph \n${printableReceipt.customerPhone}".encodeToByteArray())
//      list.add(DataForSendToPrinterPos58.printAndFeedLine())
//
//      list.add("Customer Name \n${printableReceipt.customerName}".encodeToByteArray())
//      list.add(DataForSendToPrinterPos58.printAndFeedLine())

//      list.add(DataForSendToPrinterPos58.initializePrinter())
//      list.add(DataForSendToPrinterPos58.selectCharacterSize(1))
//
//      list.add("--------------------------------".encodeToByteArray())
//      list.add(DataForSendToPrinterPos58.printAndFeedLine())
//      list.add("Items       Qty   Price  Total  ".encodeToByteArray())
//      list.add("--------------------------------".encodeToByteArray())

//      list.add(DataForSendToPrinterPos58.initializePrinter())
//
//      for (item in printableReceipt.items) {
//        list.add(printableReceipt.addOrderItemToPrintableString(item).encodeToByteArray())
//      }
//
//      list.add(DataForSendToPrinterPos58.initializePrinter())
//      list.add(DataForSendToPrinterPos58.selectAlignment(2))
//      list.add("\n".encodeToByteArray())
//
//      for (charge in printableReceipt.otherCharges) {
//        list.add(DataForSendToPrinterPos58.selectAlignment(2))
//        list.add("${charge.name} ${charge.value}\n".encodeToByteArray())
//      }
//
//      list.add("--------------------------------".encodeToByteArray())
//      list.add(DataForSendToPrinterPos58.printAndFeedLine())
//      list.add("Rs. ${printableReceipt.orderTotal}".encodeToByteArray())
//      list.add(DataForSendToPrinterPos58.printAndFeedLine())
//      list.add("--------------------------------".encodeToByteArray())
//      list.add(DataForSendToPrinterPos58.printAndFeedLine())
//      list.add(DataForSendToPrinterPos58.selectOrCancelBoldModel(1))
//      list.add(printableReceipt.deliveryType.encodeToByteArray())
//      list.add(DataForSendToPrinterPos58.printAndFeedLine())
//      list.add("--------------------------------".encodeToByteArray())
//      list.add(DataForSendToPrinterPos58.printAndFeedLine())
//
//      if (printableReceipt.address != null) {
//        list.add(DataForSendToPrinterPos58.initializePrinter())
//        list.add(DataForSendToPrinterPos58.selectCharacterSize(2))
//        list.add(printableReceipt.address.encodeToByteArray())
//      }

      if (qrCodeText != null) {
        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add(DataForSendToPrinterPos58.selectAlignment(1))
        list.add(qrCodeDataToByteArray(qrCodeText, 200)!!)
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
      }

      list.add(DataForSendToPrinterPos58.printAndFeedLine())
      list.add(DataForSendToPrinterPos58.printAndFeedLine())
      list.add(DataForSendToPrinterPos58.printAndFeedLine())
      list.add(DataForSendToPrinterPos58.printAndFeedLine())
      list
    })
    result.success(true)
  }

  private fun qrCodeDataToByteArray(data: String?, size: Int): ByteArray? {
    var byteMatrix: ByteMatrix? = null
    try {
      val hints = EnumMap<EncodeHintType, Any>(
        EncodeHintType::class.java
      )
      hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
      val code: QRCode = Encoder.encode(data, ErrorCorrectionLevel.L, hints)
      byteMatrix = code.matrix
    } catch (e: WriterException) {
      e.printStackTrace()
      return null
    }
    if (byteMatrix == null) {
      return null
    }
    val width = byteMatrix.width
    val height = byteMatrix.height
    val coefficient = (size.toFloat() / width.toFloat()).roundToInt()
    val imageWidth = width * coefficient
    val imageHeight = height * coefficient
    val bytesByLine = ceil((imageWidth.toFloat() / 8f).toDouble()).toInt()
    var i = 8
    if (coefficient < 1) {
      return initGSv0Command(0, 0)
    }
    val imageBytes = initGSv0Command(bytesByLine, imageHeight)
    for (y in 0 until height) {
      val lineBytes = ByteArray(bytesByLine)
      var x = -1
      var multipleX = coefficient
      var isBlack = false
      for (j in 0 until bytesByLine) {
        var b = 0
        for (k in 0..7) {
          if (multipleX == coefficient) {
            isBlack = ++x < width && byteMatrix[x, y].toInt() == 1
            multipleX = 0
          }
          if (isBlack) {
            b = b or (1 shl 7 - k)
          }
          ++multipleX
        }
        lineBytes[j] = b.toByte()
      }
      for (multipleY in 0 until coefficient) {
        if (imageBytes != null) {
          System.arraycopy(lineBytes, 0, imageBytes, i, lineBytes.size)
        }
        i += lineBytes.size
      }
    }
    return imageBytes
  }

  private fun initGSv0Command(bytesByLine: Int, bitmapHeight: Int): ByteArray? {
    val xH = bytesByLine / 256
    val xL = bytesByLine - xH * 256
    val yH = bitmapHeight / 256
    val yL = bitmapHeight - yH * 256
    val imageBytes = ByteArray(8 + bytesByLine * bitmapHeight)
    imageBytes[0] = 0x1D
    imageBytes[1] = 0x76
    imageBytes[2] = 0x30
    imageBytes[3] = 0x00
    imageBytes[4] = xL.toByte()
    imageBytes[5] = xH.toByte()
    imageBytes[6] = yL.toByte()
    imageBytes[7] = yH.toByte()
    return imageBytes
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


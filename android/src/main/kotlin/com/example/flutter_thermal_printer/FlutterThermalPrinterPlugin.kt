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
import net.posprinter.utils.DataForSendToPrinterTSC


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
  private var thermalPrinterDevices = mutableSetOf<BluetoothDevice>()
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
    myBinder?.WriteSendData(object : TaskCallback {
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

  private fun printOfflineOrderLabel(call: MethodCall, result: Result) {
    val qrCodeText = call.argument<String>("qr_code_text")
    val descText = call.argument<String>("desc_text")

    if (connectedThermalPrinter == null) {
      result.error("NO PRINTER FOUND", "connect to printer before print", "Try to connect to printer before printing.")
      return
    }
    myBinder!!.WriteSendData(object : TaskCallback {
      override fun OnSucceed() {
      }

      override fun OnFailed() {
      }
    }, ProcessData {
      // width = 4.0 inch, height = 2.0 inch
      val width = 2.54 * 10 * 4.0
      val height = 2.54 * 10 * 2.0
      // padding of 50mm from all sides
      val padding = 50

      val list: MutableList<ByteArray> = ArrayList()
      list.add(DataForSendToPrinterTSC.sizeBymm(2.54 * 4.0 * 10, 2.54 * 2.0 * 10))
      list.add(DataForSendToPrinterTSC.direction(0))
      list.add(DataForSendToPrinterTSC.cls())
      list.add(DataForSendToPrinterTSC.qrCode(500, 90, "M", 8, "A", 0, "M1", "S3", qrCodeText))
      //文本,简体中文是TSS24.BF2,可参考编程手册中字体的代号
      list.add(DataForSendToPrinterTSC.text(75, 50, "monospace", 0, 2, 1, "Changepay MMS Technology"))
      val descSplitContent = splitLineWithMaxCharCount(descText!!, 36)
      for (i in descSplitContent.indices) {
        list.add(DataForSendToPrinterTSC.text(40, 100 + i*30, "monospace", 0, 1, 1, descSplitContent[i]))
      }
      list.add(DataForSendToPrinterTSC.print(1, 1))
      list
    })

  }

  private fun splitLineWithMaxCharCount(s: String, n: Int = 25): List<String> {
    val result = mutableListOf<String>()
    var current = String()
    val splitArray = s.split(" ")

    for (item in splitArray) {
      if (current.length + item.length + 1 > n) {
        result.add(current)
        current = "$item "
      } else {
        current += "$item "
      }
    }
    if (current.isNotEmpty()) {
      result.add(current)
    }
    return result
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


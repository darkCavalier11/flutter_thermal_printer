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
import com.dantsu.escposprinter.EscPosPrinter
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
import net.posprinter.utils.DataForSendToPrinterTSC

/** FlutterThermalPrinterPlugin */
class FlutterThermalPrinterPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  private var printer: EscPosPrinter? = null

  private var activity: Activity? = null
  private lateinit var context: Context
  private var bluetoothAdapter: BluetoothAdapter? = null
  private var bluetoothManager: BluetoothManager? = null

  private var myBinder: IMyBinder? = null
  private var isConnectedToPrinter = false
  private var thermalPrinterDevices = ArrayList<BluetoothDevice>()
  private var connectedThermalPrinter: BluetoothDevice? = null
  private var mSerconnection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
      myBinder = service as IMyBinder
      makeLog("onServiceConnected(name: ComponentName, service: IBinder)")
    }

    override fun onServiceDisconnected(name: ComponentName) {
      makeLog("onServiceDisconnected(name: ComponentName)")
    }
  }

  fun makeLog(log: Any) {
    Log.d("ThermalPrinterPlugin", "$log")
  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_thermal_printer")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  @RequiresApi(Build.VERSION_CODES.S)
  private fun initialise() {
    makeLog("Asking bluetooth permissions")
    requestBluetoothPermission()
    makeLog("Initialising Bluetooth manager and adapter")
    bluetoothManager = getSystemService(context, BluetoothManager::class.java)
    bluetoothAdapter = bluetoothManager?.adapter
    if (bluetoothAdapter == null) {
      Toast.makeText(context, "Bluetooth adapter not found", Toast.LENGTH_SHORT).show()
    } else if (!bluetoothAdapter!!.isEnabled) {
      makeLog("Enabling bluetooth for connection with printer")
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

    val bluetoothPrintersMap = mutableListOf<Map<String, Any>>()
    for (device in allPairedDevices) {
      val majorDeviceClass: Int = device.bluetoothClass.majorDeviceClass
      val deviceClass: Int = device.bluetoothClass.deviceClass
      if (majorDeviceClass == 1536 && (deviceClass == 1664 || deviceClass == 1536)) {
        thermalPrinterDevices.add(device)
        bluetoothPrintersMap.add(BluetoothPrinter(device.address, device.name).toJson())
      }
    }
    result.success(bluetoothPrintersMap)
  }

  private fun connectToBluetoothPrinterByAddress(call: MethodCall, result: Result) {
    val address = call.argument<String>("bluetooth_printer_address")
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
          isConnectedToPrinter = true
          connectedThermalPrinter = selectedPrinter
          result.success(true)
        }

        override fun OnFailed() {
          logger("Connection failed onFailed() TaskCallback")
          connectedThermalPrinter = null
          isConnectedToPrinter = false
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

  private fun printString(@NonNull call: MethodCall, @NonNull result: Result) {
    if (printer != null) {
      val printableString = call.argument<String>("printable_string")
      printer!!.printFormattedText(printableString)
      result.success(true)
    } else {
      result.error("NO PRINTER FOUND", "connect to printer before print", "Try to connect to printer before printing.")
    }
  }
  private fun logger(text: Any) {
    Log.d("ThermalPrinter", "$text")
  }
  private fun printReceipt(@NonNull call: MethodCall, @NonNull result: Result) {
    val printableReceiptMap = call.argument<Map<String, Any>>("printable_receipt")
//    val qrCodeText = call.argument<String?>("qr_code_text")
    val gson = Gson()
    val printableReceipt = gson.fromJson(gson.toJson(printableReceiptMap), PrintableReceipt::class.java)
    Log.d("ThermalPrinter", "connection status ${isConnectedToPrinter}")
    if (myBinder == null) {
      logger("Calling bind service")
      val intent = Intent(context, PosprinterService::class.java)
      context.bindService(intent, mSerconnection, Context.BIND_AUTO_CREATE)
    }
    if (!isConnectedToPrinter) {
      connectBT(printableReceipt.printerId)
    }
//    if (printer == null) {
//      val selectedPrinter = BluetoothPrintersConnections().list?.first { printer -> printer.device.address == printableReceipt.printerId }
//      if (selectedPrinter != null) {
////        printer = EscPosPrinter(selectedPrinter.connect(), 203, 48f, 32)
//        connectBT(selectedPrinter.device.address)
//
//      }
//    }

//    printer?.printFormattedText(printableReceipt.generatePrintableString(qrCodeText = qrCodeText))
    if (isConnectedToPrinter) {
      Log.d("ThermalPrinter", "Invoking printText()")
      printText()
    }
    result.success(true)
  }

  private fun connectBT(address: String) {
    logger("Calling connect Bluetooth")
    logger("mBinder is $myBinder")
      myBinder?.ConnectBtPort(address, object : TaskCallback {
        override fun OnSucceed() {
          isConnectedToPrinter = true
        }
        override fun OnFailed() {

        }
      })
  }

  private fun printText() {

      myBinder?.WriteSendData(object : TaskCallback {
        override fun OnSucceed() {
          Toast.makeText(
            context,
            "Printing text",
            Toast.LENGTH_SHORT
          ).show()
        }

        override fun OnFailed() {
          Toast.makeText(
            context,
            "Error printing text, connection failes",
            Toast.LENGTH_SHORT
          ).show()
        }
      }, ProcessData {
        val list: MutableList<ByteArray> = ArrayList()
        //设置标签纸大小
        list.add(DataForSendToPrinterTSC.sizeBymm(50.0, 30.0))
        //设置间隙
        list.add(DataForSendToPrinterTSC.gapBymm(2.0, 0.0))
        //清除缓存
        list.add(DataForSendToPrinterTSC.cls())
        //设置方向
        list.add(DataForSendToPrinterTSC.direction(0))
        //线条
        //                    list.add(DataForSendToPrinterTSC.bar(10,10,200,3));
        //条码
        //                    list.add(DataForSendToPrinterTSC.barCode(10,15,"128",100,1,0,2,2,"abcdef12345"));
        //文本
        list.add(DataForSendToPrinterTSC.text(10, 30, "TSS24.BF2", 0, 1, 1, "abcasdjknf"))
        //打印
        list.add(DataForSendToPrinterTSC.print(1))
        list
      })

  }

  @RequiresApi(Build.VERSION_CODES.S)
  override fun onMethodCall(call: MethodCall, result: Result) {
    PermissionUtils.askForPermissions(activity!!)
    when (call.method) {
      "initialise" -> initialise()
      "getAllBluetoothPairedDevices" -> getAllBluetoothPairedDevices(call, result)
      "connectToBluetoothPrinterByAddress" -> connectToBluetoothPrinterByAddress(call, result)
      "isConnectedToBluetoothThermalPrinter" -> isConnectedToBluetoothThermalPrinter(call, result)
      "disconnectBluetoothThermalPrinter" -> disconnectBluetoothThermalPrinter(call, result)
      "printString" -> printString(call, result)
      "printReceipt" -> printReceipt(call, result)
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


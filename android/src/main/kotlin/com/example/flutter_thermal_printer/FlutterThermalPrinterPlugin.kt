package com.example.flutter_thermal_printer

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.getSystemService
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.example.flutter_thermal_printer.models.BluetoothPrinter
import com.example.flutter_thermal_printer.models.Item
import com.example.flutter_thermal_printer.models.PrintableReceipt
import com.google.gson.Gson


import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONObject
import java.lang.Integer.min

/** FlutterThermalPrinterPlugin */
class FlutterThermalPrinterPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private var printer: EscPosPrinter? = null
  private var connectedPrinterAddress: String? = null
  private var activity: Activity? = null
  private lateinit var context: Context

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_thermal_printer")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
  private fun initialise() {
    if (activity != null) {
      PermissionUtils.askForPermissions(activity!!)
    }
    val bluetoothManager: BluetoothManager? = getSystemService(context, BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    if (bluetoothAdapter == null) {
      Toast.makeText(context, "Bluetooth adapter not found", Toast.LENGTH_SHORT).show()
    } else {
      val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      startActivityForResult(activity!!,  enableBtIntent, 1, null)
    }

  }

  private fun getAllPairedDevices(@NonNull call: MethodCall, @NonNull result: Result) {
    val pairedPrinters = BluetoothPrintersConnections().list
    val bluetoothPrintersMap = mutableListOf<Map<String, Any>>()
    if (pairedPrinters != null && pairedPrinters.isNotEmpty()) {
      for (p in BluetoothPrintersConnections().list!!) {
        bluetoothPrintersMap.add(BluetoothPrinter(p.device.address, p.device.name).toJson())
      }
      result.success(bluetoothPrintersMap)
    }
    else {
      result.success(listOf<BluetoothPrinter>())
    }
  }

  private fun connectToPrinterByAddress(@NonNull call: MethodCall, @NonNull result: Result) {
    val address = call.argument<String>("printer_id")
    val selectedPrinter = BluetoothPrintersConnections().list?.first { printer -> printer.device.address == address }
    if (selectedPrinter != null) {
      printer = EscPosPrinter(selectedPrinter.connect(), 203, 48f, 32)
      connectedPrinterAddress = address
      // printing an empty line to make sure it is connected
      printer?.printFormattedText("[L]\n")
      val connectedPrinter = BluetoothPrinter(selectedPrinter.device.address, selectedPrinter.device.name)
      result.success(connectedPrinter.toJson())
    } else {
      result.error("NOT FOUND", "Unable to connect to the printer with $address", "Error occured while connecting to the printer with address $address. Make sure printer is on, and paired with the device",)
    }
  }

  private fun isConnected(@NonNull call: MethodCall, @NonNull result: Result) {
    val address = call.argument<String>("address")
    if (printer == null) {
      return result.success(false)
    }
    result.success(connectedPrinterAddress == address)
  }

  private fun disconnect(@NonNull call: MethodCall, @NonNull result: Result) {
    val address = call.argument<String>("address")
    val selectedPrinter = BluetoothPrintersConnections().list?.first { printer -> printer.device.address == address }
    if (selectedPrinter == null) {
      return
    }
    selectedPrinter.disconnect()
    printer = null
    connectedPrinterAddress = null
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

  private fun printReceipt(@NonNull call: MethodCall, @NonNull result: Result) {
    val printableReceiptMap = call.argument<Map<String, Any>>("printable_receipt")
    val gson = Gson()
    val printableReceipt = gson.fromJson(gson.toJson(printableReceiptMap), PrintableReceipt::class.java)
    if (printer == null) {
      val selectedPrinter = BluetoothPrintersConnections().list?.first { printer -> printer.device.address == printableReceipt.printerId }
      if (selectedPrinter != null) {
        printer = EscPosPrinter(selectedPrinter.connect(), 203, 48f, 32)
      }
    }
    printer?.printFormattedText(printableReceipt.generatePrintableString())
    result.success(true)
  }

  @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    PermissionUtils.askForPermissions(activity!!)
    when (call.method) {
      "initialise" -> initialise()
      "getAllPairedDevices" -> getAllPairedDevices(call, result)
      "connectToPrinterByAddress" -> connectToPrinterByAddress(call, result)
      "isConnected" -> isConnected(call, result)
      "disconnect" -> disconnect(call, result)
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

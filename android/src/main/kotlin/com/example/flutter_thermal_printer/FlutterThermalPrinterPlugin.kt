package com.example.flutter_thermal_printer

import android.util.Log
import androidx.annotation.NonNull
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.example.flutter_thermal_printer.models.BluetoothPrinter


import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** FlutterThermalPrinterPlugin */
class FlutterThermalPrinterPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private var printer: EscPosPrinter? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_thermal_printer")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "getAllPairedDevices") {
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
    } else if (call.method == "connectToPrinterByAddress") {
      val address = call.argument<String>("printer_id")
      val selectedPrinter = BluetoothPrintersConnections().list?.first { printer -> printer.device.address == address }
      if (selectedPrinter != null) {
        printer = EscPosPrinter(selectedPrinter.connect(), 203, 48f, 32)
        // printing an empty line to make sure it is connected
        printer?.printFormattedText("[L]\n")
      } else {
        result.error("NOT FOUND", "Unable to connect to the printer with $address", "Error occured while connecting to the printer with address $address. Make sure printer is on, and paired with the device",)
      }
    } else if (call.method == "isConnected") {
      result.success(printer != null)
    } else if (call.method == "disconnect") {
      printer?.disconnectPrinter()
      printer = null
    } else if (call.method == "printString") {
      if (printer != null) {
        val printableString = call.argument<String>("printable_string")
        printer!!.printFormattedText(printableString)
        result.success(true)
      } else {
        result.error("NO PRINTER FOUND", "connect to printer before print", "Try to connect to printer before printing.")
      }
    } else if (call.method == "printReceipt") {
      val printableReceipt = call.argument<String>("printable_receipt")
      Log.d("PrintableRecipt", printableReceipt.toString())
      result.success(true)
    }
    else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

}

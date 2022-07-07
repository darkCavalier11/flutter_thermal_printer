package com.example.flutter_thermal_printer

import android.util.Log
import androidx.annotation.NonNull
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.example.flutter_thermal_printer.models.BluetoothPrinter
import com.example.flutter_thermal_printer.models.Item
import com.example.flutter_thermal_printer.models.PrintableReceipt
import com.google.gson.Gson


import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONObject
import java.lang.Integer.min

/** FlutterThermalPrinterPlugin */
class FlutterThermalPrinterPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private var printer: EscPosPrinter? = null
  private val PAPER_WIDTH = 32;
  private val ITEM_NAME_WIDTH = 12;
  private val ITEM_QTY_WIDTH = 6;
  private val ITEM_PRICE_WIDTH = 7;
  private val ITEM_TOTAL_WIDTH = 7;

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_thermal_printer")
    channel.setMethodCallHandler(this)
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
      // printing an empty line to make sure it is connected
      printer?.printFormattedText("[L]\n")
    } else {
      result.error("NOT FOUND", "Unable to connect to the printer with $address", "Error occured while connecting to the printer with address $address. Make sure printer is on, and paired with the device",)
    }
  }

  private fun isConnected(@NonNull call: MethodCall, @NonNull result: Result) {
    result.success(printer != null)
  }

  private fun disconnect() {
    printer?.disconnectPrinter()
    printer = null
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

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "getAllPairedDevices" -> getAllPairedDevices(call, result)
      "connectToPrinterByAddress" -> connectToPrinterByAddress(call, result)
      "isConnected" -> isConnected(call, result)
      "disconnect" -> disconnect()
      "printString" -> printString(call, result)
      "printReceipt" -> printReceipt(call, result)
      else -> result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

}

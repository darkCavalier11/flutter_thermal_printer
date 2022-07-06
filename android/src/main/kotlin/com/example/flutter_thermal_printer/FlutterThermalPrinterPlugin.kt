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
        // printing an empty line to make sure it is connected
        printer?.printFormattedText("[L]\n")
      }
    }
    var printableString =
      "[C]<font size='big'>${printableReceipt.orderId}</font>\n" +
              "[C]<b>${printableReceipt.datetime}</b>\n" +
              "[C]<b>Sweet Shop</b>\n" +
              "[C]--------------------------------\n" +
              "<b>Items       Qty   Price  Total  </b>\n" +
              "[C]--------------------------------\n"
    for (orderItem in printableReceipt.items) {
      printableString += addOrderItemToPrintableString(orderItem)
    }
    for (charge in printableReceipt.otherCharges) {
      printableString += "[R]${charge.name} ${charge.value}\n"
    }
    printableString += "[R]--------------------------------\n"
    printableString += "[R]Rs. ${printableReceipt.orderTotal}\n"
    printableString += "[C]--------------------------------\n"
    printableString += "<b>${printableReceipt.deliveryType}</b>\n"
    printableString += "[C]--------------------------------\n"
    printableString += "[L]<font size='big'>${printableReceipt.address}</font>"
    printer?.printFormattedText(printableString)
    result.success(true)
  }

  private fun addOrderItemToPrintableString(orderItem: Item): String {
    val startIndexed = mutableListOf<Int>(0,0,0,0)
    var printableOrderItemString = ""
    while (true) {
      if (startIndexed[0] == orderItem.name.length &&
        startIndexed[1] == orderItem.quantity.toString().length &&
        startIndexed[2] == orderItem.price.toString().length &&
        startIndexed[3] == orderItem.total.toString().length) break;
      val endIndex1 = min(
        startIndexed[0] + ITEM_NAME_WIDTH - 1,
        orderItem.name.length);
      val name = orderItem.name.substring(startIndexed[0], endIndex1);
      startIndexed[0] = endIndex1;
      printableOrderItemString += name + " " + " ".repeat (ITEM_NAME_WIDTH - name.length - 1)

      val endIndex2 = min(
        startIndexed[1] + ITEM_QTY_WIDTH - 1,
        orderItem.quantity.toString().length);
      val quantity = orderItem.quantity.toString().substring(startIndexed[1], endIndex2);
      startIndexed[1] = endIndex2;
      printableOrderItemString += quantity + " " + " ".repeat (ITEM_QTY_WIDTH - quantity.length - 1)

      val endIndex3 = min(
        startIndexed[2] + ITEM_PRICE_WIDTH - 1,
        orderItem.price.toString().length);
      val price = orderItem.price.toString().substring(startIndexed[2], endIndex3);
      startIndexed[2] = endIndex3;
      printableOrderItemString += price + " " + " ".repeat (ITEM_PRICE_WIDTH - price.length - 1)

      val endIndex4 = min(
        startIndexed[3] + ITEM_TOTAL_WIDTH - 1,
        orderItem.total.toString().length);
      val total = orderItem.total.toString().substring(startIndexed[3], endIndex4);
      startIndexed[3] = endIndex4;
      printableOrderItemString += total + " " + " ".repeat (ITEM_TOTAL_WIDTH - price.length - 1)
      printableOrderItemString += '\n'
    }
    printableOrderItemString += "[C]--------------------------------\n"
    return printableOrderItemString
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

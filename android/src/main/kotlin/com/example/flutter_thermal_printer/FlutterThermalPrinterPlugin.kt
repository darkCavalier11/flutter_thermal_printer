package com.example.flutter_thermal_printer

import android.bluetooth.BluetoothAdapter
import androidx.annotation.NonNull
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import io.flutter.Log


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

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_thermal_printer")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {


    if (call.method == "getAllPairedDevices") {
      var pairedPrinters = BluetoothPrintersConnections().list
      var bluetoothPrintersMap = mutableListOf<Map<String, Any>>()
      if (pairedPrinters != null && pairedPrinters.isNotEmpty()) {
        for (p in BluetoothPrintersConnections().list!!) {
          bluetoothPrintersMap.add(BluetoothPrinter(p.device.address, p.device.name).toJson())
        }
        result.success(bluetoothPrintersMap)
      }
      else {
        result.success(listOf<BluetoothPrinter>())
      }
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

}

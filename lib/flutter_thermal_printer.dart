import 'dart:async';
import 'dart:convert';
import 'dart:developer';

import 'package:flutter/services.dart';
import 'package:flutter_thermal_printer/models/bluetooth_printer.dart';

export 'models/bluetooth_printer.dart';

class FlutterThermalPrinter {
  FlutterThermalPrinter._();
  static const MethodChannel _channel =
      MethodChannel('flutter_thermal_printer');

  static FlutterThermalPrinter instance = FlutterThermalPrinter._();

  static Future<List<BluetoothPrinter>> get getAllPairedDevices async {
    final availableDevicesMap =
        await _channel.invokeMethod("getAllPairedDevices");
    final bluetoothPrinters = <BluetoothPrinter>[];
    for (var printer in availableDevicesMap) {
      bluetoothPrinters
          .add(BluetoothPrinter.fromJson(Map<String, dynamic>.from(printer)));
    }
    return bluetoothPrinters;
  }
}

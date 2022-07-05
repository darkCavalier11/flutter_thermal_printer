import 'dart:async';

import 'package:flutter/services.dart';

class FlutterThermalPrinter {
  static const MethodChannel _channel =
      MethodChannel('flutter_thermal_printer');

  static Future<Map<String, dynamic>> get getAllPairedDevices async {
    final Map<String, dynamic> availableDevices = await _channel.invokeMethod("getAllPairedDevices");
    return availableDevices;
  }

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}

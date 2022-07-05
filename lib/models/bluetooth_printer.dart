import 'dart:developer';

import 'package:flutter/services.dart';
import 'package:flutter_thermal_printer/flutter_thermal_printer.dart';
import 'package:permission_handler/permission_handler.dart';

class BluetoothPrinter {
  final String printerId;
  final String printerName;
  BluetoothPrinter({
    required this.printerId,
    required this.printerName,
  });

  factory BluetoothPrinter.fromJson(Map<String, dynamic> json) {
    return BluetoothPrinter(
      printerId: json['printer_id'],
      printerName: json['printer_name'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "printer_id": printerId,
      "printer_name": printerName,
    };
  }

  Future<void> connect() async {
    final bluetoothConnectPermission = Permission.bluetoothConnect;

    if (await bluetoothConnectPermission.isGranted || await bluetoothConnectPermission.isLimited) {
      const MethodChannel _channel = MethodChannel('flutter_thermal_printer');
      try {
        await _channel.invokeMethod("connectToPrinterByAddress", 
          {"printer_id": printerId}
      );
      } catch (e) {
        log(e.toString());
      }
    }
  }

  @override
  String toString() =>
      'BluetoothPrinter(printerId: $printerId, printerName: $printerName)';
}

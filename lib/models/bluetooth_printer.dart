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
  static const MethodChannel _channel =
      MethodChannel('flutter_thermal_printer');

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
    const bluetoothConnectPermission = Permission.bluetoothConnect;
    final status = await bluetoothConnectPermission.request();
    if (status.isGranted || status.isLimited) {
      try {
        await _channel.invokeMethod(
            "connectToPrinterByAddress", {"printer_id": printerId});
      } catch (e) {
        log(e.toString());
      }
    }
  }

  Future<bool> printString(String printableString) async {
    try {
      await _channel
          .invokeMethod("printString", {"printable_string": printableString});
      return true;
    } catch (e) {
      log(e.toString());
      return false;
    }
  }

  Future<bool> printReceipt(PrintableReceipt receipt) async {
    try {
      await _channel.invokeMethod(
          "printReceipt", {"printable_receipt": receipt.toJson()});
      return true;
    } catch (e) {
      log(e.toString());
      return false;
    }
  }

  // This method checks if the printer is connected to any printer.
  Future<bool> isConnected() async {
    try {
      final isConnected =
          await _channel.invokeMethod("isConnected", {"address": printerId});
      return isConnected;
    } catch (e) {
      log(e.toString());
      return false;
    }
  }

  Future<void> disconnect() async {
    try {
      await _channel.invokeMethod("disconnect", {"address": printerId});
    } catch (e) {
      log(e.toString());
    }
  }

  @override
  String toString() =>
      'BluetoothPrinter(printerId: $printerId, printerName: $printerName)';
}

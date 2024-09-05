import 'dart:developer';

import 'package:flutter/services.dart';
import 'package:flutter_thermal_printer/flutter_thermal_printer.dart';
import 'package:permission_handler/permission_handler.dart';

class BluetoothPrinter {
  final String printerAddress;
  final String printerName;
  BluetoothPrinter({
    required this.printerAddress,
    required this.printerName,
  });
  static const MethodChannel _channel =
      MethodChannel('flutter_thermal_printer');

  factory BluetoothPrinter.fromJson(Map<String, dynamic> json) {
    return BluetoothPrinter(
      printerAddress: json['printer_address'],
      printerName: json['printer_name'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "printer_id": printerAddress,
      "printer_name": printerName,
    };
  }

  Future<void> connect() async {
    const bluetoothConnectPermission = Permission.bluetoothConnect;
    final status = await bluetoothConnectPermission.request();
    if (status.isGranted || status.isLimited) {
      try {
        await _channel.invokeMethod("connectToBluetoothPrinterByAddress",
            {"bluetooth_printer_address": printerAddress});
      } catch (e) {
        log(e.toString());
      }
    }
  }

  Future<bool> printString(String printableString) async {
    try {
      await _channel.invokeMethod("printStringWithBluetoothPrinter",
          {"printable_string": printableString});
      return true;
    } catch (e) {
      log(e.toString());
      return false;
    }
  }

  Future<bool> printReceipt(PrintableReceipt receipt,
      {String? qrCodeText}) async {
    try {
      final result = await _channel.invokeMethod<bool>(
        "printReceiptWithBluetoothPrinter",
        {
          "printable_receipt": receipt.toJson(),
          "qr_code_text": qrCodeText,
        },
      );
      return result ?? false;
    } catch (e) {
      log(e.toString());
      return false;
    }
  }

  Future<bool> printOfflineOrderLabel({
    required String qrCodeText,
    required String descText,
  }) async {
    try {
      final result = await _channel.invokeMethod<bool>(
        "printOfflineOrderLabel",
        {
          "qr_code_text": qrCodeText,
          "desc_text": descText,
        },
      );
      return result ?? false;
    } catch (e) {
      log(e.toString());
      return false;
    }
  }

  // This method checks if the printer is connected to any printer.
  Future<bool> isConnected() async {
    try {
      final isConnected = await _channel.invokeMethod(
          "isConnectedToBluetoothThermalPrinter",
          {"bluetooth_printer_address": printerAddress});
      return isConnected;
    } catch (e) {
      log(e.toString());
      return false;
    }
  }

  Future<void> disconnect() async {
    try {
      await _channel.invokeMethod("disconnectBluetoothThermalPrinter");
    } catch (e) {
      log(e.toString());
    }
  }

  @override
  String toString() =>
      'BluetoothPrinter(printerAddress: $printerAddress, printerName: $printerName)';
}

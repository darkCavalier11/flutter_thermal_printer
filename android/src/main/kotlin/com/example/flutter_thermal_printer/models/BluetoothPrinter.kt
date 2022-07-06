package com.example.flutter_thermal_printer.models

class BluetoothPrinter(private val printerId: String, private val printerName: String) {
    private var _printerId: String = printerId;
    private var _printerName: String = printerName;

    public fun toJson(): Map<String, Any> {
        return mapOf("printer_id" to _printerId, "printer_name" to _printerName)
    }
 }
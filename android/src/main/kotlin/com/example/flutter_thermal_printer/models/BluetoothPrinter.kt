package com.example.flutter_thermal_printer.models

class BluetoothPrinter(val printerId: String, val printerName: String) {
    lateinit var _printerId: String;
    lateinit var _printerName: String;

    init {
        _printerId = printerId
        _printerName = printerName
    }
    public fun toJson(): Map<String, Any> {
        return mapOf("printer_id" to _printerId, "printer_name" to _printerName)
    }
 }
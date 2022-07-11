import 'dart:convert';
import 'dart:developer';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_thermal_printer/flutter_thermal_printer.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List<BluetoothPrinter> _pairedDevices = [];

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    List<BluetoothPrinter> pairedDevices;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      pairedDevices = await FlutterThermalPrinter.getAllPairedDevices;
    } on PlatformException {
      pairedDevices = [];
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _pairedDevices = pairedDevices;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text(_pairedDevices.toString()),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () async {
            final p = await FlutterThermalPrinter.connectToPrinterByAddress(
                "DC:0D:30:4D:1E:0C");
          },
          child: const Icon(Icons.print),
        ),
      ),
    );
  }
}

var json = """
    {
        "printer_id": "66:12:A5:6B:97:46",
        "order_id": "b0869d",
        "datetime": "27/06/2022 15:06PM UTC",
        "delivery_type": "SMART_BOX_DELIVERY",
        "items": [
            {
                "name": "Kalakand",
                "quantity": 2,
                "price": 28000,
                "total": 56000
            }
        ],
        "other_charges": [
            {
                "name": "PACKING",
                "value": 400,
                "merchant_added": false,
                "breakup": {}
            },
            {
                "name": "EXTRA",
                "value": 1000,
                "merchant_added": false,
                "breakup": {}
            },
            {
                "name": "TAX",
                "value": 2870,
                "merchant_added": false,
                "breakup": {}
            }
        ],
        "discount": 0,
        "order_total": 60270,
        "address": "Pretty Address, eSamudaay TESTBOX"
    }
""";

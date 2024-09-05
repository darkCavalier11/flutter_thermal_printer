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
    FlutterThermalPrinter.initialise();
    List<BluetoothPrinter> pairedDevices;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      pairedDevices = await FlutterThermalPrinter.getAllBluetoothPairedDevices;
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
            final p = await FlutterThermalPrinter.getAllBluetoothPairedDevices;
            // await Future.delayed(Duration(seconds: 5));
            p[0].connect();
            p[0].printOfflineOrderLabel(
              qrCodeText: "SqXXsttz6ecRsJwoa7reAdmuuk7L2TNs5iYwnbiiev3lmV+ZPlmwe6d3wVdEK+lQw7FsxcFEyu2vX7lL0gnThIg2anO6Ocl5cPT7dDGALro=",
              descText: "Thank You for recharging with Rs. 1000 at 5 star bakery. Your credits will expire on 25 Apr 2025. Show this QR to be able to place orders at the Business or use the ChangePay app to place the orders. You can also access this QR on the ChangePay app in the Business Catalog screen. For any queries reach out to +917750860057. ",
            );
            // await p[1].disconnect();
            //  status = await p[1].isConnected();
            // log('isConnected : $status');
            // p[0].printReceipt(PrintableReceipt.fromJson(jsonDecode(json)),
            //     qrCodeText:
            //         '4lWio0SGdkjrifokjelrjierklekn==@^()))(+_fgjnreklf');
          },
          child: const Icon(Icons.print),
        ),
      ),
    );
  }
}

var json = """
    {
        "printer_id": "86:67:7A:00:15:A8",
        "order_id": "b0869d",
        "datetime": "27/06/2022 15:06PM UTC",
        "delivery_type": "SMART_BOX_DELIVERY",
        "items": [
            {
                "name": "Kalakand",
                "quantity": 2,
                "price": 28000,
                "total": 56000
            },
            {
                "name": "Kalakand sn jsjndn jsd sndjns dsdjmsjd",
                "quantity": 5585,
                "price": 2800000,
                "total": 56000000
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
        "address": "Pretty Address, eSamudaay TESTBOX",
        "customer_phone": "+91-7750860057",
        "customer_name": "Sumit"
    }
""";

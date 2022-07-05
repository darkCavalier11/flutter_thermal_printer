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

  @override
  String toString() => 'BluetoothPrinter(printerId: $printerId, printerName: $printerName)';
}

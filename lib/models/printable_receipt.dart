class PrintableCharges {
  final String name;
  final double value;
  PrintableCharges({
    required this.name,
    required this.value,
  });
  factory PrintableCharges.fromJson(Map<String, dynamic> json) {
    return PrintableCharges(
      name: json['name'] ?? '-',
      value: (json['value'] ?? 0) * 1.00,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'value': value,
    };
  }

  @override
  String toString() => 'PrintableCharges(name: $name, value: $value)';
}

class PrintableOrderItems {
  final String name;
  final double total;
  final int quantity;
  final double price;
  PrintableOrderItems({
    required this.name,
    required this.total,
    required this.quantity,
    required this.price,
  });

  factory PrintableOrderItems.fromJson(Map<String, dynamic> json) {
    return PrintableOrderItems(
        name: json['name'] ?? '-',
        total: (json['total'] ?? 0) * 1.00,
        quantity: json['quantity'] ?? 0,
        price: (json['price'] ?? 0) * 1.00);
  }

  @override
  String toString() {
    return 'PrintableOrderItems(name: $name, total: $total, quantity: $quantity, price: $price)';
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'total': total,
      'quantity': quantity,
      'price': price,
    };
  }
}

class PrintableReceipt {
  final String dateTime;
  final String address;
  final String deliveryType;
  final List<PrintableOrderItems> items;
  final List<PrintableCharges> otherCharges;
  final double discount;
  final double orderTotal;
  final String printerId;
  final String businessName;
  final String orderId;
  final String customerPhone;
  final String customerName;
  final String orderLongId;
  final String encrQrString;
  final String? customerNote;
  PrintableReceipt({
    required this.dateTime,
    required this.customerPhone,
    required this.address,
    required this.deliveryType,
    required this.items,
    required this.otherCharges,
    required this.discount,
    required this.orderTotal,
    required this.printerId,
    required this.businessName,
    required this.orderId,
    required this.orderLongId,
    required this.encrQrString,
    required this.customerName,
    required this.customerNote,
  });
  factory PrintableReceipt.fromJson(Map<String, dynamic> json) {
    return PrintableReceipt(
      orderLongId: json['order_long_id'] ?? '',
      dateTime: json['datetime'] ?? '-',
      address: json['address'] ?? '-',
      deliveryType: json['delivery_type'] ?? '-',
      businessName: json['business_name'] ?? '-',
      items: json['items'] == null
          ? []
          : (json['items'] as List)
              .map((e) => PrintableOrderItems.fromJson(e))
              .toList(),
      otherCharges: json['other_charges'] == null
          ? []
          : (json['other_charges'] as List)
              .map((e) => PrintableCharges.fromJson(e))
              .toList(),
      discount: (json['discount'] ?? 0) * 1.00,
      orderTotal: (json['order_total'] ?? 0) * 1.00,
      printerId: json['printer_id'] ?? '',
      orderId: json['order_id'] ?? '-',
      customerPhone: json['customer_phone'] ?? '-',
      encrQrString: json['encr_qr_string'] ?? '-',
      customerName: json['customer_name'] ?? '-',
      customerNote: json['customer_note'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'datetime': dateTime,
      'address': address,
      'delivery_type': deliveryType,
      'items': items.map((x) => x.toJson()).toList(),
      'other_charges': otherCharges.map((x) => x.toJson()).toList(),
      'discount': discount,
      'order_total': orderTotal,
      'printer_id': printerId,
      'business_name': businessName,
      'order_id': orderId,
      'customer_phone': customerPhone,
      'encr_qr_string': encrQrString,
      'customer_name': customerName,
      'customer_note': customerNote,
    };
  }
}

extension ConvertPaisaToRupee on num? {
  double get paisaToRupee => this == null ? 0 : this! / 100;
}

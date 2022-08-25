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
      value: json['value'] == null ? 0 : (json['value'] as int).paisaToRupee,
    );
  }

  @override
  String toString() => 'PrintableCharges(name: $name, value: $value)';

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'value': value,
    };
  }

  factory PrintableCharges.fromMap(Map<String, dynamic> map) {
    return PrintableCharges(
      name: map['name'] ?? '',
      value: map['value']?.toDouble() ?? 0.0,
    );
  }
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
      total: json['total'] == null ? 0 : (json['total'] as int).paisaToRupee,
      quantity: json['quantity'] ?? 0,
      price: json['price'] == null ? 0 : (json['price'] as int).paisaToRupee,
    );
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
  PrintableReceipt({
    required this.dateTime,
    required this.address,
    required this.deliveryType,
    required this.items,
    required this.otherCharges,
    required this.discount,
    required this.orderTotal,
    required this.printerId,
    required this.businessName,
    required this.orderId,
  });
  factory PrintableReceipt.fromJson(Map<String, dynamic> json) {
    return PrintableReceipt(
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
      discount: json['discount'] ?? 0,
      orderTotal: json['order_total'] == null
          ? 0
          : (json['order_total'] as int).paisaToRupee,
      printerId: json['printer_id'] ?? '',
      orderId: json['order_id'] ?? '-',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'date_time': dateTime,
      'address': address,
      'delivery_type': deliveryType,
      'items': items.map((x) => x.toJson()).toList(),
      'other_charges': otherCharges.map((x) => x.toJson()).toList(),
      'discount': discount,
      'order_total': orderTotal,
      'printer_id': printerId,
      'business_name': businessName,
      'order_id': orderId,
    };
  }
}

extension ConvertPaisaToRupee on num? {
  double get paisaToRupee => this == null ? 0 : this! / 100;
}

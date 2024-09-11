// ignore_for_file: public_member_api_docs, sort_constructors_first
class OfflineOrderLabel {
  final String qrCodeText;
  final String businessName;
  final String customerName;
  final String customerPhone;
  final String creditIssued;
  final String issuedOn;
  final String validTill;
  final String tokenId;
  OfflineOrderLabel({
    required this.qrCodeText,
    required this.businessName,
    required this.customerName,
    required this.customerPhone,
    required this.creditIssued,
    required this.issuedOn,
    required this.validTill,
    required this.tokenId,
  });

  @override
  String toString() {
    return 'OfflineOrderLabel(qrCodeText: $qrCodeText, businessName: $businessName, customerName: $customerName, customerPhone: $customerPhone, creditIssued: $creditIssued, issuedOn: $issuedOn, validTill: $validTill, tokenId: $tokenId)';
  }

  Map<String, dynamic> toJson() {
    return {
      'qr_code_text': qrCodeText,
      'business_name': businessName,
      'customer_name': customerName,
      'customer_phone': customerPhone,
      'credit_issued': creditIssued,
      'issued_on': issuedOn,
      'valid_till': validTill,
      'token_id': tokenId,
    };
  }
}

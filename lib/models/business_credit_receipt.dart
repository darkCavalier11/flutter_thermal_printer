// ignore_for_file: public_member_api_docs, sort_constructors_first
class BusinessCreditReceipt {
  final String creditText;
  final String qrCodeText;
  BusinessCreditReceipt({
    required this.creditText,
    required this.qrCodeText,
  });

  @override
  String toString() => 'BusinessCreditReceipt(creditText: $creditText, qrCodeText: $qrCodeText)';

  @override
  bool operator ==(covariant BusinessCreditReceipt other) {
    if (identical(this, other)) return true;
  
    return 
      other.creditText == creditText &&
      other.qrCodeText == qrCodeText;
  }

  @override
  int get hashCode => creditText.hashCode ^ qrCodeText.hashCode;

  Map<String, dynamic> toJson() {
    return {
      'credit_text': creditText,
      'qr_code_text': qrCodeText,
    };
  }
}

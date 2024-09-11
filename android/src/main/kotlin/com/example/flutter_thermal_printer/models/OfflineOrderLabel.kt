package com.example.flutter_thermal_printer.models

import com.google.gson.annotations.SerializedName

class OfflineOrderLabel (
    @SerializedName("qr_code_text")
    var qrCodeText: String,
    @SerializedName("business_name")
    var businessName: String,
    @SerializedName("customer_name")
    var customerName: String,
    @SerializedName("customer_phone")
    var customerPhone: String,
    @SerializedName("credit_issued")
    var creditIssued: String,
    @SerializedName("issued_on")
    var issuedOn: String,
    @SerializedName("valid_till")
    var validTill: String,
    @SerializedName("token_id")
    var tokenId: String,
) {
    
}
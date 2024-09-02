package com.example.flutter_thermal_printer.models

import com.google.gson.annotations.SerializedName

class BusinessCreditReceipt(
    @SerializedName("credit_text")
    val creditText: String?,
    @SerializedName("qr_code_text")
    val qrCodeText: String) {

    public fun generatePrintableString(): String {
        var printableString = "[C]<qrcode size='20'>${qrCodeText}</qrcode>\n"
        printableString += "[C]\n\n\n"
        printableString += "[C]<b>${creditText}</b>\n"
        return printableString
    }
}
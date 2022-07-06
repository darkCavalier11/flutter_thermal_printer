package com.example.flutter_thermal_printer.models

import com.google.gson.annotations.SerializedName


class PrintableReceipt(
    val address: String,
    @SerializedName("date_time")
    val datetime: String,
    @SerializedName("delivery_type")
    val deliveryType: String,
    val discount: Double,
    val items: List<Item>,
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("order_total")
    val orderTotal: Double,
    @SerializedName("other_charges")
    val otherCharges: List<OtherCharge>,
    @SerializedName("printer_id")
    val printerId: String
)

class Item(
    val name: String,
    val price: Double,
    val quantity: Int,
    val total: Double
)

data class OtherCharge(
    val name: String,
    val value: Double,
)


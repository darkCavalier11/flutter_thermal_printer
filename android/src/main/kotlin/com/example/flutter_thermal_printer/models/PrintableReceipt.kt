package com.example.flutter_thermal_printer.models
import com.google.gson.annotations.SerializedName


data class PrintableReceipt(
    @SerializedName("address")
    val address: String,
    @SerializedName("datetime")
    val datetime: String,
    @SerializedName("delivery_type")
    val deliveryType: String,
    @SerializedName("discount")
    val discount: Int,
    @SerializedName("items")
    val items: List<Item>,
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("order_total")
    val orderTotal: Int,
    @SerializedName("other_charges")
    val otherCharges: List<OtherCharge>,
    @SerializedName("printer_id")
    val printerId: String
)

data class Item(
    @SerializedName("name")
    val name: String,
    @SerializedName("price")
    val price: Int,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("total")
    val total: Int
)

data class OtherCharge(
    @SerializedName("breakup")
    val breakup: Breakup,
    @SerializedName("merchant_added")
    val merchantAdded: Boolean,
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val value: Int
)

class Breakup
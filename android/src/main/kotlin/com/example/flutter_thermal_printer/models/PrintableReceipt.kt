package com.example.flutter_thermal_printer.models

import android.util.Log
import com.google.gson.annotations.SerializedName
import java.lang.Integer.min


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


) {

    public fun generatePrintableString(): String {
        var printableString =
            "[C]<font size='big'>${orderId}</font>\n" +
                    "[C]<b>${datetime}</b>\n" +
                    "[C]<b>Sweet Shop</b>\n" +
                    "[C]--------------------------------\n" +
                    "<b>Items       Qty   Price  Total  </b>\n" +
                    "[C]--------------------------------\n"
        for (orderItem in items) {
            printableString += addOrderItemToPrintableString(orderItem)
        }
        for (charge in otherCharges) {
            printableString += "[R]${charge.name} ${charge.value}\n"
        }
        printableString += "[R]--------------------------------\n"
        printableString += "[R]Rs. ${orderTotal}\n"
        printableString += "[C]--------------------------------\n"
        printableString += "<b>${deliveryType}</b>\n"
        printableString += "[C]--------------------------------\n"
        printableString += "[L]<font size='big'>${address}</font>"
        return printableString
    }
    private fun addOrderItemToPrintableString(orderItem: Item): String {
        val ITEM_NAME_WIDTH = 12;
        val ITEM_QTY_WIDTH = 6;
        val ITEM_PRICE_WIDTH = 7;
        val ITEM_TOTAL_WIDTH = 7;
        val startIndexed = mutableListOf<Int>(0,0,0,0)
        var printableOrderItemString = ""
        while (true) {
            if (startIndexed[0] == orderItem.name.length &&
                startIndexed[1] == orderItem.quantity.toString().length &&
                startIndexed[2] == orderItem.price.toString().length &&
                startIndexed[3] == orderItem.total.toString().length) break;
            val endIndex1 = min(
                startIndexed[0] + ITEM_NAME_WIDTH - 1,
                orderItem.name.length);
            Log.d("Taghaga", ITEM_NAME_WIDTH.toString())
            Log.d("Taghaga", orderItem.name.length.toString())

            Log.d("Taghaga", startIndexed[0].toString())
            Log.d("Taghaga", endIndex1.toString())
            val name = orderItem.name.substring(startIndexed[0], endIndex1);


            startIndexed[0] = endIndex1;
            printableOrderItemString += name + " " + " ".repeat (ITEM_NAME_WIDTH - name.length - 1)

            val endIndex2 = min(
                startIndexed[1] + ITEM_QTY_WIDTH - 1,
                orderItem.quantity.toString().length);
            val quantity = orderItem.quantity.toString().substring(startIndexed[1], endIndex2);
            startIndexed[1] = endIndex2;
            printableOrderItemString += quantity + " " + " ".repeat (ITEM_QTY_WIDTH - quantity.length - 1)

            val endIndex3 = min(
                startIndexed[2] + ITEM_PRICE_WIDTH - 1,
                orderItem.price.toString().length);
            val price = orderItem.price.toString().substring(startIndexed[2], endIndex3);
            startIndexed[2] = endIndex3;
            printableOrderItemString += price + " " + " ".repeat (ITEM_PRICE_WIDTH - price.length - 1)

            val endIndex4 = min(
                startIndexed[3] + ITEM_TOTAL_WIDTH - 1,
                orderItem.total.toString().length);
            val total = orderItem.total.toString().substring(startIndexed[3], endIndex4);
            startIndexed[3] = endIndex4;
            printableOrderItemString += total + " " + " ".repeat (ITEM_TOTAL_WIDTH - price.length - 1)
            printableOrderItemString += '\n'
        }
        printableOrderItemString += "[C]--------------------------------"
        return printableOrderItemString
    }
}

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


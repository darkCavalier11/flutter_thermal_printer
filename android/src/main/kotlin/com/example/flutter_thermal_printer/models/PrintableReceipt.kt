package com.example.flutter_thermal_printer.models

import com.google.gson.annotations.SerializedName
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.encoder.QRCode
import net.posprinter.utils.DataForSendToPrinterPos58
import net.posprinter.utils.DataForSendToPrinterPos80
import java.lang.Integer.min
import java.util.EnumMap
import kotlin.math.ceil
import kotlin.math.roundToInt


class PrintableReceipt(
    @SerializedName("address")
    val address: String?,
    @SerializedName("datetime")
    val datetime: String,
    @SerializedName("delivery_type")
    val deliveryType: String,
    val items: List<CartItem>,
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("order_total")
    val orderTotal: Double,
    @SerializedName("other_charges")
    val otherCharges: List<OtherCharge>,
    @SerializedName("printer_id")
    val printerId: String,
    @SerializedName("business_name")
    val businessName: String,
    @SerializedName("customer_phone")
    val customerPhone: String,
    @SerializedName("customer_name")
    val customerName: String,
    @SerializedName("customer_note")
    val customerNote: String?,
    ) {

    public fun generatePrintableByteArrayForPaperWidth58(qrCodeText: String? = null): MutableList<ByteArray> {
        val list: MutableList<ByteArray> = java.util.ArrayList()
        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add(DataForSendToPrinterPos58.selectAlignment(1))
        list.add(DataForSendToPrinterPos58.selectCharacterSize(18))
        list.add(orderId.encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())

        list.add(DataForSendToPrinterPos58.selectCharacterSize(16))
        list.add(datetime.encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())

        list.add(businessName.encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add(DataForSendToPrinterPos58.selectOrCancelBoldModel(1))

        list.add("Customer Ph \n${customerPhone}".encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())

        list.add("Customer Name \n${customerName}".encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())

        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add(DataForSendToPrinterPos58.selectCharacterSize(1))

        if (qrCodeText != null) {
            list.add(DataForSendToPrinterPos58.initializePrinter())
            list.add(DataForSendToPrinterPos58.printAndFeedLine())
            list.add(DataForSendToPrinterPos58.printAndFeedLine())
            list.add(DataForSendToPrinterPos58.printAndFeedLine())
            list.add(DataForSendToPrinterPos58.printAndFeedLine())
            list.add(DataForSendToPrinterPos58.selectAlignment(1))
            list.add(qrCodeDataToByteArray(qrCodeText, 250)!!)
            list.add(DataForSendToPrinterPos58.printAndFeedLine())
            list.add(DataForSendToPrinterPos58.printAndFeedLine())
            list.add(DataForSendToPrinterPos58.printAndFeedLine())
            list.add(DataForSendToPrinterPos58.printAndFeedLine())

        }

        list.add("--------------------------------".encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add("Items       Qty   Price  Total  ".encodeToByteArray())
        list.add("--------------------------------".encodeToByteArray())

        list.add(DataForSendToPrinterPos58.initializePrinter())

        for (item in items) {
            list.add(addOrderItemToPrintableString(item).encodeToByteArray())
        }

        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add(DataForSendToPrinterPos58.selectAlignment(2))
        list.add("\n".encodeToByteArray())

        if (customerNote != null) {
            list.add(DataForSendToPrinterPos58.initializePrinter())
            list.add(DataForSendToPrinterPos58.selectAlignment(2))
            list.add("Note: $customerNote".encodeToByteArray())
            list.add("\n".encodeToByteArray())
            list.add("--------------------------------".encodeToByteArray())
            list.add("\n".encodeToByteArray())
        }


        for (charge in otherCharges) {
            list.add(DataForSendToPrinterPos58.selectAlignment(2))
            list.add("${charge.name} ${charge.value}\n".encodeToByteArray())
        }

        list.add("--------------------------------".encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add("Rs. ${orderTotal}".encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add("--------------------------------".encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add(DataForSendToPrinterPos58.selectOrCancelBoldModel(1))
        list.add(deliveryType.encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add("--------------------------------".encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())

        if (address != null) {
            list.add(DataForSendToPrinterPos58.initializePrinter())
            list.add(DataForSendToPrinterPos58.selectCharacterSize(2))
            list.add(address.encodeToByteArray())
            list.add(DataForSendToPrinterPos58.printAndFeedLine())
        }

        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        return list
    }

    public fun generatePrintableByteArrayForPaperWidth80(qrCodeText: String? = null): MutableList<ByteArray> {
        val list: MutableList<ByteArray> = java.util.ArrayList()
        list.add(DataForSendToPrinterPos80.initializePrinter())
        list.add(DataForSendToPrinterPos80.selectAlignment(1))
        list.add(DataForSendToPrinterPos80.selectCharacterSize(18))
        list.add(orderId.encodeToByteArray())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())

        list.add(DataForSendToPrinterPos80.selectCharacterSize(16))
        list.add(datetime.encodeToByteArray())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())

        list.add(businessName.encodeToByteArray())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        list.add(DataForSendToPrinterPos80.selectOrCancelBoldModel(1))

        list.add("Customer Ph \n${customerPhone}".encodeToByteArray())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())

        list.add("Customer Name \n${customerName}".encodeToByteArray())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())

        list.add(DataForSendToPrinterPos80.initializePrinter())
        list.add(DataForSendToPrinterPos80.selectCharacterSize(1))

        if (qrCodeText != null) {
            list.add(DataForSendToPrinterPos80.initializePrinter())
            list.add(DataForSendToPrinterPos80.selectAlignment(1))
            list.add(qrCodeDataToByteArray(qrCodeText, 250)!!)
            list.add(DataForSendToPrinterPos80.printAndFeedLine())
        }

        list.add("--------------------------------".encodeToByteArray())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        list.add("Items       Qty   Price  Total  ".encodeToByteArray())
        list.add("--------------------------------".encodeToByteArray())

        list.add(DataForSendToPrinterPos80.initializePrinter())

        for (item in items) {
            list.add(addOrderItemToPrintableString(item).encodeToByteArray())
        }

        list.add(DataForSendToPrinterPos80.initializePrinter())
        list.add(DataForSendToPrinterPos80.selectAlignment(2))
        list.add("\n".encodeToByteArray())

        if (customerNote != null) {
            list.add(DataForSendToPrinterPos80.initializePrinter())
            list.add(DataForSendToPrinterPos80.selectAlignment(2))
            list.add("Note: $customerNote".encodeToByteArray())
            list.add("\n".encodeToByteArray())
            list.add("--------------------------------".encodeToByteArray())
            list.add("\n".encodeToByteArray())
        }


        for (charge in otherCharges) {
            list.add(DataForSendToPrinterPos80.selectAlignment(2))
            list.add("${charge.name} ${charge.value}\n".encodeToByteArray())
        }

        list.add("--------------------------------".encodeToByteArray())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        list.add("Rs. ${orderTotal}".encodeToByteArray())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        list.add("--------------------------------".encodeToByteArray())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        list.add(DataForSendToPrinterPos80.selectOrCancelBoldModel(1))
        list.add(deliveryType.encodeToByteArray())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        list.add("--------------------------------".encodeToByteArray())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())

        if (address != null) {
            list.add(DataForSendToPrinterPos80.initializePrinter())
            list.add(DataForSendToPrinterPos80.selectCharacterSize(2))
            list.add(address.encodeToByteArray())
            list.add(DataForSendToPrinterPos80.printAndFeedLine())
        }

        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
        return list
    }

    private fun qrCodeDataToByteArray(data: String?, size: Int): ByteArray? {
        var byteMatrix: ByteMatrix? = null
        try {
            val hints = EnumMap<EncodeHintType, Any>(
                EncodeHintType::class.java
            )
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            val code: QRCode = Encoder.encode(data, ErrorCorrectionLevel.L, hints)
            byteMatrix = code.matrix
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }
        if (byteMatrix == null) {
            return null
        }
        val width = byteMatrix.width
        val height = byteMatrix.height
        val coefficient = (size.toFloat() / width.toFloat()).roundToInt()
        val imageWidth = width * coefficient
        val imageHeight = height * coefficient
        val bytesByLine = ceil((imageWidth.toFloat() / 8f).toDouble()).toInt()
        var i = 8
        if (coefficient < 1) {
            return initGSv0Command(0, 0)
        }
        val imageBytes = initGSv0Command(bytesByLine, imageHeight)
        for (y in 0 until height) {
            val lineBytes = ByteArray(bytesByLine)
            var x = -1
            var multipleX = coefficient
            var isBlack = false
            for (j in 0 until bytesByLine) {
                var b = 0
                for (k in 0..7) {
                    if (multipleX == coefficient) {
                        isBlack = ++x < width && byteMatrix[x, y].toInt() == 1
                        multipleX = 0
                    }
                    if (isBlack) {
                        b = b or (1 shl 7 - k)
                    }
                    ++multipleX
                }
                lineBytes[j] = b.toByte()
            }
            for (multipleY in 0 until coefficient) {
                if (imageBytes != null) {
                    System.arraycopy(lineBytes, 0, imageBytes, i, lineBytes.size)
                }
                i += lineBytes.size
            }
        }
        return imageBytes
    }

    private fun initGSv0Command(bytesByLine: Int, bitmapHeight: Int): ByteArray? {
        val xH = bytesByLine / 256
        val xL = bytesByLine - xH * 256
        val yH = bitmapHeight / 256
        val yL = bitmapHeight - yH * 256
        val imageBytes = ByteArray(8 + bytesByLine * bitmapHeight)
        imageBytes[0] = 0x1D
        imageBytes[1] = 0x76
        imageBytes[2] = 0x30
        imageBytes[3] = 0x00
        imageBytes[4] = xL.toByte()
        imageBytes[5] = xH.toByte()
        imageBytes[6] = yL.toByte()
        imageBytes[7] = yH.toByte()
        return imageBytes
    }
    fun addOrderItemToPrintableString(orderItem: CartItem): String {
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
        printableOrderItemString += "--------------------------------"
        return printableOrderItemString
    }
}

class CartItem(
    val name: String,
    val price: Double,
    val quantity: Int,
    val total: Double
)

data class OtherCharge(
    val name: String,
    val value: Double,
)

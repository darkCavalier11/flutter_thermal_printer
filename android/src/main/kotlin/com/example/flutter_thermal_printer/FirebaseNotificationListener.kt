package com.example.flutter_thermal_printer

import android.app.NotificationChannel
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.example.flutter_thermal_printer.models.BluetoothPrinter
import com.example.flutter_thermal_printer.models.PrintableReceipt
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson

class FirebaseNotificationListener: FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, remoteMessage.data.toString())
            Log.d(TAG,remoteMessage.data["printable_receipt"].toString())
            Log.d(TAG, Gson().toJson(remoteMessage).toString())
//            if (remoteMessage.data["printable_receipt"] != null) {
//                val gson = Gson()
//                val printableReceipt = gson.fromJson(gson.toJson(remoteMessage.data["printable_receipt"]), PrintableReceipt::class.java)
//                val selectedPrinter = BluetoothPrintersConnections().list?.first { printer -> printer.device.address == printableReceipt.address}
//                if (selectedPrinter != null) {
//                    val connectedPrinter = EscPosPrinter(selectedPrinter.connect(), 203, 48f, 32)
//                    connectedPrinter.printFormattedText(printableReceipt.generatePrintableString())
//                }
//            }
        }
    }
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
    companion object {

        private const val TAG = "NotificationListener"
    }
}
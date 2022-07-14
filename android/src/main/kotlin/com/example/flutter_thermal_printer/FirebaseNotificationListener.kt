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
        Log.d(TAG, "Received Notification")
        if (remoteMessage.data.isNotEmpty()) {
            if (remoteMessage.data["printable_receipt"] != null) {
                Log.d(TAG, "Printing receipt")
                val gson = Gson()
                val printableReceipt = gson.fromJson(remoteMessage.data["printable_receipt"], PrintableReceipt::class.java)
                val selectedPrinter = BluetoothPrintersConnections().list?.first { printer -> printer.device.address == printableReceipt.printerId }
                if (selectedPrinter != null) {
                    val connectedPrinter = EscPosPrinter(selectedPrinter.connect(), 203, 48f, 32)
                    connectedPrinter.printFormattedText(printableReceipt.generatePrintableString())
                }
            } else {
                Log.d(TAG, "No printable receipt found")
            }
        }
    }
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
    companion object {

        private const val TAG = "NotificationListener"
    }
}
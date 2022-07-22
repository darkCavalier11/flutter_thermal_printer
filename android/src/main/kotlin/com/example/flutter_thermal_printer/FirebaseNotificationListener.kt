package com.example.flutter_thermal_printer

import android.Manifest
import android.app.NotificationChannel
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.example.flutter_thermal_printer.models.BluetoothPrinter
import com.example.flutter_thermal_printer.models.PrintableReceipt
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson

class FirebaseNotificationListener: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.getExtras() == null) {
            Log.d(
                TAG,
                "broadcast received but intent contained no extras to process RemoteMessage. Operation cancelled.");
            return
        }
        val remoteMessage = RemoteMessage(intent?.getExtras())
        Log.d(TAG, "Received Notification")
        if (remoteMessage.data["printable_string"] != null) {
            val gson = Gson()
            Log.d(TAG, remoteMessage.data["printable_string"].toString())
            val printableReceipt = gson.fromJson(
                remoteMessage.data["printable_string"],
                PrintableReceipt::class.java
            )

            if (ActivityCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
                return
            }
            else {
                val selectedPrinter =
                    BluetoothPrintersConnections().list?.first { printer -> printer.device.address == printableReceipt.printerId }
                Log.d(TAG, selectedPrinter?.device?.name.toString())
                if (selectedPrinter != null) {
                    val printerConnection: BluetoothConnection?
                    try {
                        printerConnection = selectedPrinter.connect()
                        val connectedPrinter = EscPosPrinter(printerConnection, 203, 48f, 32)
                        Log.d(TAG, "Printing in progress...")
                        connectedPrinter.printFormattedText(printableReceipt.generatePrintableString())
                    } catch (e: Exception) {
                        Log.d(TAG, e.toString())
                    }
                }
            }

        }
    }
    companion object {
        private const val TAG = "NotificationListener"
    }
}
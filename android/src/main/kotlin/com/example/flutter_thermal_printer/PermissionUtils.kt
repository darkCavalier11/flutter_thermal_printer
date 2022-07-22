package com.example.flutter_thermal_printer

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG

object PermissionUtils {
    fun askForPermissions(activity: Activity) {
        Log.d(TAG, "Asking Permission")
        val permissionsToAsk: MutableList<String> = ArrayList()
        val requestResult = 0
        if (ContextCompat.checkSelfPermission(
                activity.applicationContext,
                Manifest.permission.BLUETOOTH
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Ask for permission
            permissionsToAsk.add(Manifest.permission.BLUETOOTH)
        }
        if (ContextCompat.checkSelfPermission(
                activity.applicationContext,
                Manifest.permission.BLUETOOTH_ADMIN
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Ask for permission
            permissionsToAsk.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (ContextCompat.checkSelfPermission(
                activity.applicationContext,
                Manifest.permission.BLUETOOTH_SCAN
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Ask for permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionsToAsk.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
        if (ContextCompat.checkSelfPermission(
                activity.applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Ask for permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionsToAsk.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        if (permissionsToAsk.size > 0) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToAsk.toTypedArray(),
                requestResult
            )
        }
    }
}
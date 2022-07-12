package com.example.flutter_thermal_printer_example

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import java.lang.Exception

class SideActivity: FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        Log.d("ComeOn!", "HelloW@orld")
        throw Exception("Blah!")
    }
}
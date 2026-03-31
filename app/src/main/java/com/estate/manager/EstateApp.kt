package com.estate.manager

import android.app.Application
import com.chaquo.python.android.AndroidPlatform
import com.chaquo.python.Python

class EstateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Start Python runtime once for the entire app lifetime
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}

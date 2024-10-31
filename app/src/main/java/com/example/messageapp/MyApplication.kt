package com.example.messageapp

import android.app.Application
import android.os.StrictMode
import com.example.messageapp.helper.screenHeight
import com.example.messageapp.helper.screenWidth
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setUpScreenSize()

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private fun setUpScreenSize() {
        resources.displayMetrics.run {
            screenWidth = this.widthPixels
            screenHeight = this.heightPixels
        }
    }
}
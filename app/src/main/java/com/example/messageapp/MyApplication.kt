package com.example.messageapp

import android.app.Application
import android.os.StrictMode
import com.example.messageapp.helper.screenHeight
import com.example.messageapp.helper.screenWidth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setUpScreenSize()
    }

    fun subscribeToToken(token: String) {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        FirebaseMessaging.getInstance().subscribeToTopic(token)
    }

    private fun setUpScreenSize() {
        resources.displayMetrics.run {
            screenWidth = this.widthPixels
            screenHeight = this.heightPixels
        }
    }
}
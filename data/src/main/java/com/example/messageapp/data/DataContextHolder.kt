package com.example.messageapp.data

import android.content.Context

object DataContextHolder {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}

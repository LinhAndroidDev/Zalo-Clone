package com.example.messageapp.remote.request

data class NotificationData(
    val token: String? = null,
    val notification: Data? = null
)

data class Data(
    val title: String,
    val body: String
)
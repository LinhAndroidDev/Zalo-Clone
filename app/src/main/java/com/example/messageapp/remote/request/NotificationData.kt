package com.example.messageapp.remote.request

data class NotificationData(
    val token: String? = null,
    val notification: Notification? = null,
    val data: Data? = null
)

data class Notification(
    val title: String,
    val body: String
)

data class Data(
    val senderId: String
)
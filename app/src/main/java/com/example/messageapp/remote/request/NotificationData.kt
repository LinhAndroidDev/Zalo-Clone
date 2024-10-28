package com.example.messageapp.remote.request

import java.util.HashMap

data class NotificationData(
    val token: String? = null,
    val data: HashMap<String, String>
)
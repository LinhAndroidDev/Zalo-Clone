package com.example.messageapp.data.firestore

data class ChatThreadPin(
    val messageTime: String = "",
    val pinnedBy: String = "",
    val pinnedByName: String = "",
    val previewText: String = "",
    val messageType: Int = 0,
    val photoUrl: String? = null,
)

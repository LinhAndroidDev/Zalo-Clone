package com.example.messageapp.domain.model

data class PinnedMessage(
    val messageTime: String = "",
    val pinnedBy: String = "",
    val pinnedByName: String = "",
    val previewText: String = "",
    val messageType: Int = 0,
    val photoUrl: String? = null,
)

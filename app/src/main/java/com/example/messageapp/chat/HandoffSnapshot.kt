package com.example.messageapp.chat

data class HandoffSnapshot(
    val videoUrl: String?,
    val positionMs: Long,
    val sameMediaAlreadyLoaded: Boolean,
)

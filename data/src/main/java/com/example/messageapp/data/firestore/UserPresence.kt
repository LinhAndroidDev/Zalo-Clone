package com.example.messageapp.data.firestore

data class UserPresence(
    val online: Boolean = false,
    val lastSeen: Long = 0L,
)

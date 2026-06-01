package com.example.messageapp.model

data class UserPresence(
    val online: Boolean = false,
    val lastSeen: Long = 0L,
)

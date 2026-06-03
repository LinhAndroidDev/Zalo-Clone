package com.example.messageapp.data.firestore

data class Friend(
    val name: String = "",
    val avatar: String = "",
    val keyAuth: String = "",
    val since: Long = 0L
)
package com.example.messageapp.data.firestore

data class MemberReadCursor(
    val userId: String = "",
    val lastReadTime: String = "",
)

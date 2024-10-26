package com.example.messageapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val name: String? = "",
    val email: String? = "",
    val avatar: String? = "",
    val keyAuth: String? = ""
) : Parcelable {
    constructor(conversation: Conversation) : this(
        name = conversation.name,
        email = "",
        avatar = conversation.friendImage,
        keyAuth = conversation.friendId
    )
}
package com.example.messageapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Conversation(
    val friendId: String = "",
    val friendImage: String = "",
    var message: String = "",
    val name: String = "",
    var person: String = "",
    var sender: String = "",
    var time: String = "",
    var seen: String = "0",
    var numberUnSeen: Int = 0
) : Parcelable {
    constructor(user: User) : this (
        friendId = user.keyAuth ?: "",
        friendImage = user.avatar ?: "",
        message = "",
        name = user.name ?: "",
        person = "",
        sender = "",
        time = "",
        seen = "0",
    )

    fun isSeenMessage(): Boolean {
        return seen == "1"
    }

    fun isMessageFromFriend(): Boolean {
        return sender == friendId
    }
}
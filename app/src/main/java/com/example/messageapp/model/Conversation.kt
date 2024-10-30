package com.example.messageapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Conversation(
    val friendId: String = "",
    val friendImage: String = "",
    val message: String = "",
    val name: String = "",
    val person: String = "",
    val sender: String = "",
    val time: String = ""
) : Parcelable
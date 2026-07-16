package com.example.messageapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PinnedMessage(
    val messageTime: String = "",
    val pinnedBy: String = "",
    val pinnedByName: String = "",
    val previewText: String = "",
    val messageType: Int = 0,
    val photoUrl: String? = null,
) : Parcelable

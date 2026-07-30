package com.example.messageapp.argument

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatVideoPlayerArgument(
    val videoUrl: String,
    val messageTime: String = "",
    val startPositionMs: Long = 0L,
) : Parcelable

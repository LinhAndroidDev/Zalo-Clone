package com.example.messageapp.chat

import androidx.media3.exoplayer.ExoPlayer

interface ChatVideoPlayTarget {
    val videoUrl: String
    fun attachPlayer(player: ExoPlayer)
    fun detachPlayer()
    fun visibleRatio(): Float
}

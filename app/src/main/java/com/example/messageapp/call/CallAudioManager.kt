package com.example.messageapp.call

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/** Chế độ âm thanh cho cuộc gọi: MODE_IN_COMMUNICATION, focus, loa ngoài. */
class CallAudioManager(context: Context) {
    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var previousMode = AudioManager.MODE_NORMAL
    private var previousSpeaker = false
    private var active = false

    fun start(speakerOn: Boolean) {
        if (active) return
        active = true
        previousMode = audioManager.mode
        previousSpeaker = audioManager.isSpeakerphoneOn
        requestFocus()
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        setSpeaker(speakerOn)
    }

    @Suppress("DEPRECATION")
    fun setSpeaker(on: Boolean) {
        audioManager.isSpeakerphoneOn = on
    }

    @Suppress("DEPRECATION")
    fun stop() {
        if (!active) return
        active = false
        audioManager.isSpeakerphoneOn = previousSpeaker
        audioManager.mode = previousMode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            audioManager.abandonAudioFocus(null)
        }
        focusRequest = null
    }

    @Suppress("DEPRECATION")
    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }
}

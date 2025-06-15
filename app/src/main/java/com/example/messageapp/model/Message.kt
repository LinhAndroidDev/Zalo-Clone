package com.example.messageapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Message(
    val message: String = "",
    val receiver: String = "",
    val sender: String = "",
    val time: String = "",
    var emotion: Emotion? = null,
    val photos: ArrayList<String> = arrayListOf(),
    val singlePhoto: ArrayList<String> = arrayListOf(),
    val audio: String? = null,
    val type: Int = 0 // 0: message, 1: photos, 2: single photo, 3: audio
) : Parcelable

@Parcelize
data class Emotion(
    val favourite: Map<String, Int> = mapOf(),
    val like: Map<String, Int> = mapOf(),
    val laugh: Map<String, Int> = mapOf(),
    val cry: Map<String, Int> = mapOf(),
    val angry: Map<String, Int> = mapOf(),
) : Parcelable {
    fun emotionEmpty(): Boolean {
        return favourite.isEmpty() && like.isEmpty() && laugh.isEmpty() && cry.isEmpty() && angry.isEmpty()
    }

    fun totalQuantityEmotion(): Int {
        return favourite.size + like.size + laugh.size + cry.size + angry.size
    }
}
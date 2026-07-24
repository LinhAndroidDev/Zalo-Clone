package com.example.messageapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StoryMediaTransform(
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val translationXNorm: Float = 0f,
    val translationYNorm: Float = 0f,
) : Parcelable {
    companion object {
        val Default = StoryMediaTransform()
    }
}

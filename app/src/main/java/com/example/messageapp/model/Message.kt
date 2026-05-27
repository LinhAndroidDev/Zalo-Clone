package com.example.messageapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageMention(
    val userId: String = "",
    val token: String = "",
    val displayName: String = "",
) : Parcelable

@Parcelize
data class Message(
    val message: String = "",
    val receiver: String = "",
    val sender: String = "",
    val time: String = "",
    var emotion: Emotion? = null,
    val mentions: List<MessageMention> = emptyList(),
    val photos: ArrayList<String> = arrayListOf(),
    /** Cùng thứ tự với [photos], mỗi phần tử dạng "widthxheight" (px sau khi xử lý rotation đối với video). */
    val photoSizes: ArrayList<String>? = null,
    val singlePhoto: ArrayList<String> = arrayListOf(),
    val audio: String? = null,
    val type: Int = 0 // 0: message, 1: photos, 2: single photo, 3: audio
) : Parcelable

enum class EmotionType(val firestoreKey: String) {
    FAVOURITE("favourite"),
    LIKE("like"),
    LAUGH("laugh"),
    CRY("cry"),
    ANGRY("angry"),
    ;

    internal fun reactions(emotion: Emotion): Map<String, Int> = when (this) {
        FAVOURITE -> emotion.favourite
        LIKE -> emotion.like
        LAUGH -> emotion.laugh
        CRY -> emotion.cry
        ANGRY -> emotion.angry
    }

    internal fun withReactions(emotion: Emotion, reactions: Map<String, Int>): Emotion = when (this) {
        FAVOURITE -> emotion.copy(favourite = reactions)
        LIKE -> emotion.copy(like = reactions)
        LAUGH -> emotion.copy(laugh = reactions)
        CRY -> emotion.copy(cry = reactions)
        ANGRY -> emotion.copy(angry = reactions)
    }
}

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

    fun findUserReaction(userId: String): EmotionType? {
        if (userId.isBlank()) return null
        return EmotionType.entries.firstOrNull { type -> userId in type.reactions(this) }
    }

    fun removeUserReaction(userId: String): Emotion {
        if (userId.isBlank()) return this
        return EmotionType.entries.fold(this) { emotion, type ->
            type.withReactions(emotion, type.reactions(emotion) - userId)
        }
    }

    fun applyUserReaction(userId: String, type: EmotionType): Emotion {
        if (userId.isBlank()) return this
        val cleared = removeUserReaction(userId)
        val updated = type.reactions(cleared) + (userId to 1)
        return type.withReactions(cleared, updated)
    }

    fun toggleUserReaction(userId: String, type: EmotionType): Emotion {
        if (userId.isBlank()) return this
        return if (findUserReaction(userId) == type) {
            removeUserReaction(userId)
        } else {
            applyUserReaction(userId, type)
        }
    }
}
package com.example.messageapp.domain.model

data class MessageMention(
    val userId: String = "",
    val token: String = "",
    val displayName: String = "",
)

data class MessageReply(
    val messageTime: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val previewText: String = "",
    val type: Int = 0,
    val photoUrl: String? = null,
)

data class Message(
    val message: String = "",
    val receiver: String = "",
    val sender: String = "",
    val time: String = "",
    val emotion: Emotion? = null,
    val mentions: List<MessageMention> = emptyList(),
    val photos: List<String> = emptyList(),
    val photoSizes: List<String>? = null,
    val singlePhoto: List<String> = emptyList(),
    val audio: String? = null,
    val type: Int = 0,
    val replyTo: MessageReply? = null,
)

enum class EmotionType() {
    FAVOURITE,
    LIKE,
    LAUGH,
    CRY,
    ANGRY;

    fun reactions(emotion: Emotion): Map<String, Int> = when (this) {
        FAVOURITE -> emotion.favourite
        LIKE -> emotion.like
        LAUGH -> emotion.laugh
        CRY -> emotion.cry
        ANGRY -> emotion.angry
    }

    fun withReactions(emotion: Emotion, reactions: Map<String, Int>): Emotion = when (this) {
        FAVOURITE -> emotion.copy(favourite = reactions)
        LIKE -> emotion.copy(like = reactions)
        LAUGH -> emotion.copy(laugh = reactions)
        CRY -> emotion.copy(cry = reactions)
        ANGRY -> emotion.copy(angry = reactions)
    }
}

data class Emotion(
    val favourite: Map<String, Int> = emptyMap(),
    val like: Map<String, Int> = emptyMap(),
    val laugh: Map<String, Int> = emptyMap(),
    val cry: Map<String, Int> = emptyMap(),
    val angry: Map<String, Int> = emptyMap(),
) {
    fun emotionEmpty(): Boolean =
        favourite.isEmpty() && like.isEmpty() && laugh.isEmpty() && cry.isEmpty() && angry.isEmpty()

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

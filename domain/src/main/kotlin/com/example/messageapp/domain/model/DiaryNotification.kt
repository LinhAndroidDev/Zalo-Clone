package com.example.messageapp.domain.model

enum class DiaryNotificationType {
    POST_REACTION,
    POST_COMMENT,
    COMMENT_LIKE,
    COMMENT_REPLY,
    REPLY_LIKE,
}

data class DiaryNotification(
    val id: String = "",
    val type: DiaryNotificationType = DiaryNotificationType.POST_COMMENT,
    val actorId: String = "",
    val actorName: String = "",
    val actorAvatarUrl: String = "",
    val postId: String = "",
    val commentId: String = "",
    val replyId: String = "",
    val reactionType: EmotionType? = null,
    val postPreviewText: String = "",
    val postThumbnailUrl: String = "",
    val commentPreviewText: String = "",
    val read: Boolean = false,
    val createdAtMillis: Long = 0L,
)

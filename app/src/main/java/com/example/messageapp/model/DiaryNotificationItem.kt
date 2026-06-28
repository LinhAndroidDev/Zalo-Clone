package com.example.messageapp.model

import com.example.messageapp.domain.model.DiaryNotificationType
import com.example.messageapp.domain.model.EmotionType

data class DiaryNotificationItem(
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
    val actionText: String = "",
    val timeText: String = "",
)

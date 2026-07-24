package com.example.messageapp.mapper

import android.content.Context
import com.example.messageapp.R
import com.example.messageapp.domain.model.DiaryNotification
import com.example.messageapp.domain.model.DiaryNotificationType
import com.example.messageapp.domain.model.EmotionType
import com.example.messageapp.model.DiaryNotificationItem
import com.example.messageapp.utils.RelativeTimeFormatter

object DiaryNotificationUiMapper {

    fun toUiItems(context: Context, notifications: List<DiaryNotification>): List<DiaryNotificationItem> =
        notifications.map { toUi(context, it) }

    fun toUi(context: Context, notification: DiaryNotification): DiaryNotificationItem {
        return DiaryNotificationItem(
            id = notification.id,
            type = notification.type,
            actorId = notification.actorId,
            actorName = notification.actorName,
            actorAvatarUrl = notification.actorAvatarUrl,
            postId = notification.postId,
            commentId = notification.commentId,
            replyId = notification.replyId,
            reactionType = notification.reactionType,
            postPreviewText = notification.postPreviewText,
            postThumbnailUrl = notification.postThumbnailUrl,
            commentPreviewText = notification.commentPreviewText,
            read = notification.read,
            createdAtMillis = notification.createdAtMillis,
            actionText = formatActionText(context, notification),
            timeText = RelativeTimeFormatter.format(context, notification.createdAtMillis),
        )
    }

    private fun formatActionText(context: Context, n: DiaryNotification): String = when (n.type) {
        DiaryNotificationType.POST_REACTION -> {
            val reaction = reactionLabel(context, n.reactionType)
            context.getString(R.string.diary_notif_post_reaction, reaction)
        }
        DiaryNotificationType.POST_COMMENT ->
            context.getString(R.string.diary_notif_post_comment)
        DiaryNotificationType.COMMENT_LIKE ->
            context.getString(R.string.diary_notif_comment_like)
        DiaryNotificationType.COMMENT_REPLY ->
            context.getString(R.string.diary_notif_comment_reply)
        DiaryNotificationType.REPLY_LIKE ->
            context.getString(R.string.diary_notif_reply_like)
    }

    private fun reactionLabel(context: Context, type: EmotionType?): String = when (type) {
        EmotionType.FAVOURITE -> context.getString(R.string.diary_reaction_favourite)
        EmotionType.LIKE -> context.getString(R.string.diary_reaction_like)
        EmotionType.LAUGH -> context.getString(R.string.diary_reaction_laugh)
        EmotionType.CRY -> context.getString(R.string.diary_reaction_cry)
        EmotionType.ANGRY -> context.getString(R.string.diary_reaction_angry)
        null -> context.getString(R.string.diary_reaction_like)
    }

    fun formatRelativeTime(context: Context, createdAtMillis: Long): String =
        RelativeTimeFormatter.format(context, createdAtMillis)

    fun actionIconRes(type: DiaryNotificationType): Int = when (type) {
        DiaryNotificationType.POST_REACTION -> R.drawable.emotion_like
        DiaryNotificationType.POST_COMMENT -> R.drawable.ic_comment
        DiaryNotificationType.COMMENT_LIKE -> R.drawable.ic_like
        DiaryNotificationType.COMMENT_REPLY -> R.drawable.ic_comment
        DiaryNotificationType.REPLY_LIKE -> R.drawable.ic_like
    }

    fun reactionIconRes(type: EmotionType?): Int = when (type) {
        EmotionType.FAVOURITE -> R.drawable.emotion_favourite
        EmotionType.LIKE -> R.drawable.emotion_like
        EmotionType.LAUGH -> R.drawable.emotion_laugh
        EmotionType.CRY -> R.drawable.emotion_cry
        EmotionType.ANGRY -> R.drawable.emotion_angry
        null -> R.drawable.emotion_like
    }
}

package com.example.messageapp.data.firestore

import com.example.messageapp.domain.model.DiaryNotification
import com.example.messageapp.domain.model.DiaryNotificationType
import com.example.messageapp.domain.model.EmotionType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

object DiaryNotificationFirestore {
    const val SUB_COLLECTION = "diaryNotifications"

    const val FIELD_TYPE = "type"
    const val FIELD_ACTOR_ID = "actorId"
    const val FIELD_ACTOR_NAME = "actorName"
    const val FIELD_ACTOR_AVATAR = "actorAvatarUrl"
    const val FIELD_POST_ID = "postId"
    const val FIELD_COMMENT_ID = "commentId"
    const val FIELD_REPLY_ID = "replyId"
    const val FIELD_REACTION_TYPE = "reactionType"
    const val FIELD_POST_PREVIEW = "postPreviewText"
    const val FIELD_POST_THUMBNAIL = "postThumbnailUrl"
    const val FIELD_COMMENT_PREVIEW = "commentPreviewText"
    const val FIELD_READ = "read"
    const val FIELD_CREATED_AT = "createdAt"

    fun fromDocument(doc: DocumentSnapshot): DiaryNotification? {
        val data = doc.data ?: return null
        val typeRaw = data[FIELD_TYPE]?.toString().orEmpty()
        val type = runCatching { DiaryNotificationType.valueOf(typeRaw) }
            .getOrElse { DiaryNotificationType.POST_COMMENT }
        val reactionRaw = data[FIELD_REACTION_TYPE]?.toString().orEmpty()
        val reactionType = reactionRaw.takeIf { it.isNotBlank() }
            ?.let { runCatching { EmotionType.valueOf(it) }.getOrNull() }
        val createdAtMillis = when (val t = data[FIELD_CREATED_AT]) {
            is Timestamp -> t.toDate().time
            is Number -> t.toLong()
            else -> System.currentTimeMillis()
        }
        return DiaryNotification(
            id = doc.id,
            type = type,
            actorId = data[FIELD_ACTOR_ID]?.toString().orEmpty(),
            actorName = data[FIELD_ACTOR_NAME]?.toString().orEmpty(),
            actorAvatarUrl = data[FIELD_ACTOR_AVATAR]?.toString().orEmpty(),
            postId = data[FIELD_POST_ID]?.toString().orEmpty(),
            commentId = data[FIELD_COMMENT_ID]?.toString().orEmpty(),
            replyId = data[FIELD_REPLY_ID]?.toString().orEmpty(),
            reactionType = reactionType,
            postPreviewText = data[FIELD_POST_PREVIEW]?.toString().orEmpty(),
            postThumbnailUrl = data[FIELD_POST_THUMBNAIL]?.toString().orEmpty(),
            commentPreviewText = data[FIELD_COMMENT_PREVIEW]?.toString().orEmpty(),
            read = data[FIELD_READ] as? Boolean ?: false,
            createdAtMillis = createdAtMillis,
        )
    }
}

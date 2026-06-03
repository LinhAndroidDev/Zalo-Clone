package com.example.messageapp.data.legacy

import com.example.messageapp.data.firestore.Message
import com.example.messageapp.data.firestore.TypeMessage

object ReplyNotificationHelper {

    private const val PREVIEW_MAX_LENGTH = 80
    private const val REPLY_PREVIEW_FORMAT = "Trả lời: %1\$s"

    fun formatInboxPreview(message: Message, basePreview: String): String {
        val reply = message.replyTo ?: return basePreview
        val quoted = reply.previewText.ifBlank { basePreview }
        return REPLY_PREVIEW_FORMAT.format(quoted)
    }

    fun formatNotificationBody(message: Message, baseBody: String): String =
        formatInboxPreview(message, baseBody)

    fun buildFcmReplyFields(
        message: Message,
        messageTime: String,
        senderName: String,
    ): FcmReplyFields = FcmReplyFields(
        messageTime = messageTime,
        replyPreviewText = buildPreviewText(message),
        replySenderName = senderName,
        replyType = resolveMessageType(message).rawValue.toString(),
        replyPhotoUrl = firstPhotoUrl(message),
    )

    data class FcmReplyFields(
        val messageTime: String,
        val replyPreviewText: String,
        val replySenderName: String,
        val replyType: String,
        val replyPhotoUrl: String?,
    )

    private fun buildPreviewText(message: Message): String {
        val type = resolveMessageType(message)
        val base = when (type) {
            TypeMessage.MESSAGE, TypeMessage.SYSTEM -> message.message
            TypeMessage.PHOTOS -> "${message.photos.size} ảnh"
            TypeMessage.SINGLE_PHOTO -> "Ảnh"
            TypeMessage.AUDIO -> "Tin thoại"
        }
        return truncate(base)
    }

    private fun resolveMessageType(message: Message): TypeMessage = TypeMessage.of(message.type)

    private fun firstPhotoUrl(message: Message): String? =
        message.singlePhoto.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: message.photos.firstOrNull()?.takeIf { it.isNotBlank() }

    private fun truncate(text: String): String {
        val trimmed = text.trim()
        if (trimmed.length <= PREVIEW_MAX_LENGTH) return trimmed
        return trimmed.take(PREVIEW_MAX_LENGTH) + "…"
    }
}

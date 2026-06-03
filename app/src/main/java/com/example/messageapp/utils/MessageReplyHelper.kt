package com.example.messageapp.utils

import android.content.Context
import android.view.View
import android.text.TextUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.model.Message
import com.example.messageapp.model.MessageReply
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.helper.screenWidth
import com.example.messageapp.utils.FileUtils.isLikelyVideoUrl
import com.example.messageapp.utils.FileUtils.loadImg

object MessageReplyHelper {

    private const val PREVIEW_MAX_LENGTH = 80
    private val userDisplayNameCache = hashMapOf<String, String>()

    data class ReplyNameContext(
        val myUserId: String,
        val myName: String,
        val peerUserId: String,
        val peerDisplayName: String,
        val isGroup: Boolean,
        val groupMembers: List<MentionHelper.MentionCandidate>,
    )

    fun isStoredNameUnresolved(storedName: String, senderId: String): Boolean {
        if (storedName.isBlank()) return true
        if (storedName == senderId) return true
        return false
    }

    fun resolveSenderName(
        senderId: String,
        myUserId: String,
        myName: String,
        groupMembers: List<MentionHelper.MentionCandidate>,
        peerUserId: String = "",
        peerDisplayName: String = "",
    ): String {
        if (senderId.isBlank()) return ""
        if (senderId == myUserId) return myName.ifBlank { "Bạn" }
        groupMembers.firstOrNull { it.userId == senderId }
            ?.displayName
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        if (peerUserId.isNotBlank() && senderId == peerUserId) {
            return peerDisplayName.ifBlank { "" }
        }
        return ""
    }

    fun resolveReplyDisplayName(
        senderId: String,
        storedName: String,
        context: ReplyNameContext,
    ): String {
        if (!isStoredNameUnresolved(storedName, senderId)) return storedName
        return resolveSenderName(
            senderId = senderId,
            myUserId = context.myUserId,
            myName = context.myName,
            groupMembers = context.groupMembers,
            peerUserId = if (!context.isGroup) context.peerUserId else "",
            peerDisplayName = if (!context.isGroup) context.peerDisplayName else "",
        )
    }

    fun cachedUserDisplayName(userId: String): String? = userDisplayNameCache[userId]

    fun fetchUserDisplayName(userId: String, onResult: (String) -> Unit) {
        if (userId.isBlank()) {
            onResult("")
            return
        }
        userDisplayNameCache[userId]?.let {
            onResult(it)
            return
        }
        FireBaseInstance.getInfoUser(userId) { user ->
            val name = user.name.orEmpty().trim()
            if (name.isNotBlank()) {
                userDisplayNameCache[userId] = name
                onResult(name)
            } else {
                onResult("")
            }
        }
    }

    fun resolveMessageType(message: Message): TypeMessage {
        return when {
            message.type in TypeMessage.entries.map { it.rawValue } ->
                TypeMessage.of(message.type)
            !message.audio.isNullOrBlank() -> TypeMessage.AUDIO
            message.singlePhoto.isNotEmpty() -> TypeMessage.SINGLE_PHOTO
            message.photos.isNotEmpty() -> TypeMessage.PHOTOS
            else -> TypeMessage.MESSAGE
        }
    }

    fun buildPreviewText(context: Context, message: Message): String {
        return when (resolveMessageType(message)) {
            TypeMessage.MESSAGE, TypeMessage.SYSTEM -> truncate(message.message)
            TypeMessage.SINGLE_PHOTO -> {
                val url = message.singlePhoto.firstOrNull().orEmpty()
                if (isLikelyVideoUrl(url)) {
                    context.getString(R.string.reply_video)
                } else {
                    context.getString(R.string.reply_photo)
                }
            }
            TypeMessage.PHOTOS -> {
                context.getString(R.string.reply_photos_count, message.photos.size)
            }
            TypeMessage.AUDIO -> context.getString(R.string.reply_audio)
        }
    }

    fun firstPhotoUrl(message: Message): String? {
        return when (resolveMessageType(message)) {
            TypeMessage.SINGLE_PHOTO -> message.singlePhoto.firstOrNull()?.takeIf { it.isNotBlank() }
            TypeMessage.PHOTOS -> message.photos.firstOrNull()?.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    fun buildMessageReply(context: Context, message: Message, senderName: String): MessageReply {
        return MessageReply(
            messageTime = message.time,
            senderId = message.sender,
            senderName = senderName,
            previewText = buildPreviewText(context, message),
            type = resolveMessageType(message).rawValue,
            photoUrl = firstPhotoUrl(message),
        )
    }

    fun bindReplyQuote(
        context: Context,
        quoteRoot: View,
        tvQuoteSender: TextView,
        tvQuotePreview: TextView,
        imgQuoteThumb: ImageView?,
        reply: MessageReply?,
        nameContext: ReplyNameContext,
        onQuoteClick: ((String) -> Unit)?,
    ) {
        if (reply == null) {
            quoteRoot.isVisible = false
            return
        }
        quoteRoot.isVisible = true

        val photoUrl = reply.photoUrl?.takeIf { it.isNotBlank() }
        val hasThumb = photoUrl != null && reply.type != TypeMessage.AUDIO.rawValue
        val maxTextWidth = quoteTextMaxWidth(context, hasThumb)

        val syncName = resolveReplyDisplayName(reply.senderId, reply.senderName, nameContext)
        if (syncName.isNotBlank()) {
            tvQuoteSender.text = ellipsizeSingleLine(syncName, tvQuoteSender, maxTextWidth)
            tvQuoteSender.isVisible = true
        } else {
            tvQuoteSender.text = ""
            tvQuoteSender.isVisible = false
            val requestUserId = reply.senderId
            fetchUserDisplayName(requestUserId) { fetchedName ->
                if (tvQuoteSender.tag != requestUserId) return@fetchUserDisplayName
                if (fetchedName.isBlank()) return@fetchUserDisplayName
                tvQuoteSender.text = ellipsizeSingleLine(fetchedName, tvQuoteSender, maxTextWidth)
                tvQuoteSender.isVisible = true
            }
        }
        tvQuoteSender.tag = reply.senderId

        tvQuotePreview.text = ellipsizeSingleLine(reply.previewText, tvQuotePreview, maxTextWidth)
        tvQuotePreview.maxLines = 1
        tvQuotePreview.ellipsize = TextUtils.TruncateAt.END

        val thumb = imgQuoteThumb
        if (thumb != null) {
            if (hasThumb && photoUrl != null) {
                thumb.isVisible = true
                context.loadImg(photoUrl, thumb)
            } else {
                thumb.isVisible = false
            }
        }

        val clickTarget = quoteRoot
        if (onQuoteClick != null && reply.messageTime.isNotBlank()) {
            clickTarget.setOnClickListener { onQuoteClick(reply.messageTime) }
            clickTarget.isClickable = true
        } else {
            clickTarget.setOnClickListener(null)
            clickTarget.isClickable = false
        }
    }

    private fun truncate(text: String): String {
        val trimmed = text.trim()
        if (trimmed.length <= PREVIEW_MAX_LENGTH) return trimmed
        return trimmed.take(PREVIEW_MAX_LENGTH) + "…"
    }

    /** Chiều rộng tối đa cho text trong quote block (px). */
    private fun quoteTextMaxWidth(context: Context, hasThumb: Boolean): Int {
        val density = context.resources.displayMetrics.density
        val quoteChromePx = (27 * density).toInt()
        val thumbExtraPx = if (hasThumb) (48 * density).toInt() else 0
        return (maxReplyQuoteTextWidth(context) - quoteChromePx - thumbExtraPx)
            .coerceAtLeast((48 * density).toInt())
    }

    private fun ellipsizeSingleLine(text: String, textView: TextView, maxWidthPx: Int): CharSequence {
        if (text.isBlank() || maxWidthPx <= 0) return text
        val available = maxWidthPx - textView.paddingLeft - textView.paddingRight
        if (available <= 0) return text
        val paint = textView.paint
        if (paint.measureText(text) <= available) return text
        return TextUtils.ellipsize(
            text,
            paint,
            available.toFloat(),
            TextUtils.TruncateAt.END,
        ) ?: text
    }

    private fun maxReplyQuoteTextWidth(context: Context): Int {
        val width = if (screenWidth > 0) {
            screenWidth
        } else {
            context.resources.displayMetrics.widthPixels
        }
        val res = context.resources
        val reserved = res.getDimensionPixelSize(R.dimen.margin_100) +
            res.getDimensionPixelSize(R.dimen.margin_50) +
            res.getDimensionPixelSize(R.dimen.margin_50)
        return (width - reserved).coerceAtLeast(res.getDimensionPixelSize(R.dimen.margin_100))
    }
}

package com.example.messageapp.broadcast

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.messageapp.R
import com.example.messageapp.data.mapper.EntityMapper
import com.example.messageapp.mapper.ChatUiMapper
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Message
import com.example.messageapp.model.MessageReply
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.service.ReceiverMessageService
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReply : BroadcastReceiver() {
    @Inject
    lateinit var shared: SharePreferenceRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val remoteInput = RemoteInput.getResultsFromIntent(intent) ?: return
        val repliedText = remoteInput.getCharSequence(ReceiverMessageService.KEY_REPLY_TEXT)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (repliedText.isBlank()) return

        val senderId = intent.getStringExtra(ReceiverMessageService.SENDER_ID).orEmpty()
        val groupId = intent.getStringExtra(ReceiverMessageService.GROUP_ID)?.trim().orEmpty()
        val groupName = intent.getStringExtra(ReceiverMessageService.GROUP_NAME).orEmpty()
        val userId = shared.getAuth()
        val time = DateUtils.getTimeCurrent()
        val replyTo = buildReplyToFromIntent(intent, senderId)

        val message = Message(
            message = repliedText,
            receiver = groupId.ifEmpty { senderId },
            sender = userId,
            time = time,
            replyTo = replyTo,
        )

        if (groupId.isNotEmpty()) {
            val uiConversation = Conversation(
                friendId = groupId,
                name = groupName,
                isGroup = true,
            )
            FireBaseInstance.sendMessage(
                message = EntityMapper.toFirestore(ChatUiMapper.toDomain(message)),
                userId = userId,
                time = time,
                conversation = EntityMapper.toFirestore(ChatUiMapper.toDomain(uiConversation)),
                nameSender = shared.getNameUser(),
                sendFirst = false,
            ) {
                showRepliedNotification(context, groupName.ifBlank { groupId })
            }
            return
        }

        FireBaseInstance.getInfoUser(userId = senderId) { fsUser ->
            fsUser.keyAuth = senderId
            val uiUser = ChatUiMapper.toUi(EntityMapper.toDomain(fsUser))
            FireBaseInstance.sendMessage(
                message = EntityMapper.toFirestore(ChatUiMapper.toDomain(message)),
                userId = userId,
                time = time,
                conversation = EntityMapper.toFirestore(ChatUiMapper.toDomain(Conversation(uiUser))),
                nameSender = shared.getNameUser(),
                sendFirst = false,
            ) {
                showRepliedNotification(context, uiUser.name.orEmpty().ifBlank { senderId })
            }
        }
    }

    private fun buildReplyToFromIntent(intent: Intent, senderId: String): MessageReply? {
        val messageTime = intent.getStringExtra(ReceiverMessageService.MESSAGE_TIME)?.trim().orEmpty()
        if (messageTime.isBlank()) return null

        val previewText = intent.getStringExtra(ReceiverMessageService.REPLY_PREVIEW_TEXT).orEmpty()
        val senderName = intent.getStringExtra(ReceiverMessageService.REPLY_SENDER_NAME).orEmpty()
        val type = intent.getStringExtra(ReceiverMessageService.REPLY_TYPE)?.toIntOrNull()
            ?: TypeMessage.MESSAGE.rawValue
        val photoUrl = intent.getStringExtra(ReceiverMessageService.REPLY_PHOTO_URL)

        return MessageReply(
            messageTime = messageTime,
            senderId = senderId,
            senderName = senderName,
            previewText = previewText,
            type = type,
            photoUrl = photoUrl,
        )
    }

    private fun showRepliedNotification(context: Context, targetName: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val repliedNotification =
            NotificationCompat.Builder(context, context.getString(R.string.title_app))
                .setSmallIcon(R.drawable.ic_message)
                .setContentText(
                    String.format(context.getString(R.string.replied_to_message_of), targetName),
                )
                .build()
        notificationManager.notify(shared.getChannelId(), repliedNotification)
    }
}

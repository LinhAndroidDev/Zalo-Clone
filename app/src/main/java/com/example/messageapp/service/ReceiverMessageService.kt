package com.example.messageapp.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.messageapp.MainActivity
import com.example.messageapp.R
import com.example.messageapp.broadcast.NotificationReply
import com.example.messageapp.domain.repository.GroupChatRepository
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.social.GetUserInfoUseCase
import com.example.messageapp.mapper.ChatUiMapper
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.User
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import java.util.Random
import javax.inject.Inject

@AndroidEntryPoint
class ReceiverMessageService : FirebaseMessagingService() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var groupChatRepository: GroupChatRepository
    @Inject lateinit var getUserInfoUseCase: GetUserInfoUseCase

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (sessionRepository.getStatusLoggedIn()) {
            val data = remoteMessage.data
            if (data.isEmpty()) return
            Log.d(TAG, "Message data payload: $data")
            val title = data["title"]
            val body = data["body"]
            val diaryNotificationType = data["diaryNotificationType"]?.trim().orEmpty()
            if (diaryNotificationType.isNotEmpty()) {
                val currentUserId = sessionRepository.getAuth().trim()
                val senderId = data["senderId"]?.trim().orEmpty()
                if (currentUserId.isNotBlank() && senderId.isNotBlank() && senderId == currentUserId) {
                    Log.d(TAG, "Skip diary push triggered by current user ($currentUserId)")
                    return
                }
                val recipientUserId = data["recipientUserId"]?.trim().orEmpty()
                if (recipientUserId.isNotBlank() && currentUserId.isNotBlank() && recipientUserId != currentUserId) {
                    Log.d(TAG, "Skip diary push for $recipientUserId (logged in as $currentUserId)")
                    return
                }
                sendDiaryNotification(
                    title = title,
                    messageBody = body,
                    postId = data["postId"].orEmpty(),
                )
                return
            }

            val currentUserId = sessionRepository.getAuth().trim()
            val senderId = data["senderId"]?.trim().orEmpty()
            if (currentUserId.isNotBlank() && senderId.isNotBlank() && senderId == currentUserId) {
                Log.d(TAG, "Skip chat push triggered by current user ($currentUserId)")
                return
            }
            val recipientUserId = data["recipientUserId"]?.trim().orEmpty()
            if (recipientUserId.isNotBlank() && currentUserId.isNotBlank() && recipientUserId != currentUserId) {
                Log.d(TAG, "Skip chat push for $recipientUserId (logged in as $currentUserId)")
                return
            }
            val groupId = data["groupId"]?.trim().orEmpty()
            val isMention = data["isMention"] == "1"
            val replyMeta = parseReplyMeta(data)

            if (groupId.isNotEmpty()) {
                groupChatRepository.getGroup(
                    groupId,
                    onSuccess = { group ->
                        val conv = Conversation(
                            friendId = groupId,
                            friendImage = group.photoUrl,
                            message = "",
                            name = group.name,
                            person = "",
                            sender = "",
                            time = "",
                            isGroup = true,
                        )
                        sendGroupNotification(
                            title = title,
                            messageBody = body,
                            conversation = conv,
                            senderId = senderId,
                            replyMeta = replyMeta,
                            isMention = isMention,
                        )
                    },
                    onFailure = { Log.e(TAG, "getGroup failed: $it") },
                )
            } else {
                getUserInfoUseCase(
                    userId = senderId,
                    onSuccess = { user ->
                        sendNotification(
                            title,
                            body,
                            senderId,
                            ChatUiMapper.toUi(user),
                            replyMeta,
                        )
                    },
                    onFailure = { Log.e(TAG, "getInfoUser failed: $it") },
                )
            }
        }
    }

    @SuppressLint("ServiceCast")
    private fun sendDiaryNotification(
        title: String?,
        messageBody: String?,
        postId: String,
    ) {
        val channelId = Random().nextInt()
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_DIARY_POST_ID, postId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            channelId,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.diary_notification_channel))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title ?: getString(R.string.diary_notifications_title))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        showDiaryNotificationChannel(channelId, notificationBuilder)
    }

    private fun showDiaryNotificationChannel(channelId: Int, notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.diary_notification_channel),
                getString(R.string.diary_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }
        sessionRepository.saveChannelId(channelId)
        notificationManager.notify(channelId, notificationBuilder.build())
    }

    @SuppressLint("ServiceCast")
    private fun sendNotification(
        title: String?,
        messageBody: String?,
        senderId: String,
        friend: User,
        replyMeta: ReplyNotificationMeta,
    ) {
        val channelId = Random().nextInt()
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(OBJECT_FRIEND, friend)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            channelId,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )

        val replyAction = buildReplyAction(
            requestCode = channelId,
            senderId = senderId,
            groupId = null,
            replyMeta = replyMeta,
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.title_app))
            .setSmallIcon(R.drawable.ic_message)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .addAction(replyAction)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        showNotification(channelId, notificationBuilder)
    }

    @SuppressLint("ServiceCast")
    private fun sendGroupNotification(
        title: String?,
        messageBody: String?,
        conversation: Conversation,
        senderId: String,
        replyMeta: ReplyNotificationMeta,
        isMention: Boolean = false,
    ) {
        val channelId = Random().nextInt()
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(OBJECT_GROUP_CONVERSATION, conversation)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            channelId,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentTitle = if (isMention) {
            getString(R.string.notification_mention_title, conversation.name)
        } else {
            title
        }

        val replyAction = buildReplyAction(
            requestCode = channelId + 1,
            senderId = senderId,
            groupId = conversation.friendId,
            groupName = conversation.name,
            replyMeta = replyMeta,
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.title_app))
            .setSmallIcon(R.drawable.ic_message)
            .setContentTitle(contentTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .addAction(replyAction)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        showNotification(channelId, notificationBuilder)
    }

    private fun buildReplyAction(
        requestCode: Int,
        senderId: String,
        groupId: String?,
        groupName: String? = null,
        replyMeta: ReplyNotificationMeta,
    ): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(KEY_REPLY_TEXT)
            .setLabel(getString(R.string.reply))
            .build()

        val replyIntent = Intent(this, NotificationReply::class.java).apply {
            putExtra(SENDER_ID, senderId)
            if (!groupId.isNullOrBlank()) {
                putExtra(GROUP_ID, groupId)
                putExtra(GROUP_NAME, groupName.orEmpty())
            }
            attachReplyMeta(this, replyMeta)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            replyIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_reply,
            getString(R.string.reply),
            replyPendingIntent,
        ).addRemoteInput(remoteInput).build()
    }

    private fun showNotification(channelId: Int, notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.title_app),
                "Channel human readable title",
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }

        sessionRepository.saveChannelId(channelId)
        notificationManager.notify(channelId, notificationBuilder.build())
    }

    private fun parseReplyMeta(data: Map<String, String>): ReplyNotificationMeta {
        return ReplyNotificationMeta(
            messageTime = data["messageTime"]?.takeIf { it.isNotBlank() },
            replyPreviewText = data["replyPreviewText"]?.takeIf { it.isNotBlank() },
            replySenderName = data["replySenderName"]?.takeIf { it.isNotBlank() },
            replyType = data["replyType"]?.takeIf { it.isNotBlank() },
            replyPhotoUrl = data["replyPhotoUrl"]?.takeIf { it.isNotBlank() },
        )
    }

    private fun attachReplyMeta(intent: Intent, meta: ReplyNotificationMeta) {
        meta.messageTime?.let { intent.putExtra(MESSAGE_TIME, it) }
        meta.replyPreviewText?.let { intent.putExtra(REPLY_PREVIEW_TEXT, it) }
        meta.replySenderName?.let { intent.putExtra(REPLY_SENDER_NAME, it) }
        meta.replyType?.let { intent.putExtra(REPLY_TYPE, it) }
        meta.replyPhotoUrl?.let { intent.putExtra(REPLY_PHOTO_URL, it) }
    }

    data class ReplyNotificationMeta(
        val messageTime: String? = null,
        val replyPreviewText: String? = null,
        val replySenderName: String? = null,
        val replyType: String? = null,
        val replyPhotoUrl: String? = null,
    )

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        const val KEY_REPLY_TEXT = "KEY_REPLY_TEXT"
        const val SENDER_ID = "SENDER_ID"
        const val GROUP_ID = "GROUP_ID"
        const val GROUP_NAME = "GROUP_NAME"
        const val MESSAGE_TIME = "MESSAGE_TIME"
        const val REPLY_PREVIEW_TEXT = "REPLY_PREVIEW_TEXT"
        const val REPLY_SENDER_NAME = "REPLY_SENDER_NAME"
        const val REPLY_TYPE = "REPLY_TYPE"
        const val REPLY_PHOTO_URL = "REPLY_PHOTO_URL"
        const val OBJECT_FRIEND = "OBJECT_FRIEND"
        const val OBJECT_GROUP_CONVERSATION = "OBJECT_GROUP_CONVERSATION"
        const val EXTRA_DIARY_POST_ID = "EXTRA_DIARY_POST_ID"
    }
}

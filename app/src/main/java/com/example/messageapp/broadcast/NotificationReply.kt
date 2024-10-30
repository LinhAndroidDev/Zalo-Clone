package com.example.messageapp.broadcast

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.messageapp.R
import com.example.messageapp.model.Message
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
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if(remoteInput != null) {
            val repliedText = remoteInput.getString(ReceiverMessageService.KEY_REPLY_TEXT)
            val receiverId = intent?.getStringExtra(ReceiverMessageService.SENDER_ID)
            val userId = shared.getAuth()
            val time = DateUtils.getTimeCurrent()
            val message = Message(
                message = repliedText.toString(),
                receiver = receiverId.toString(),
                sender = userId,
                time = time
            )

            FireBaseInstance.getInfoUser(keyAuth = receiverId.toString()) { user ->
                FireBaseInstance.sendMessage(
                    message = message,
                    keyAuth = userId,
                    time = time,
                    friend = user,
                    shared.getNameUser(),
                    shared.getAvatarUser()
                )
            }

            val notificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val repliedNotification =
                NotificationCompat
                    .Builder(context, context.getString(R.string.title_app))
                    .setSmallIcon(R.drawable.ic_message)
                    .setContentText("Đã trả lời").build()
            notificationManager.notify(shared.getChannelId(), repliedNotification)
        }
    }
}
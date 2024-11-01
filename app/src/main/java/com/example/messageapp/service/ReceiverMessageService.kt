package com.example.messageapp.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.messageapp.MainActivity
import com.example.messageapp.R
import com.example.messageapp.broadcast.NotificationReply
import com.example.messageapp.model.User
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import java.util.Random
import javax.inject.Inject

@AndroidEntryPoint
class ReceiverMessageService : FirebaseMessagingService() {

    @Inject
    lateinit var shared: SharePreferenceRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        var senderId = ""
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]
            senderId = remoteMessage.data["senderId"] ?: ""
            FireBaseInstance.getInfoUser(senderId) { user ->
                user.keyAuth = senderId
                sendNotification(title, body, senderId, user)
            }
        }

//      Check if message contains a notification payload.
//        remoteMessage.notification?.let {
//            Log.d(TAG, "Message Notification Body: ${it.body}")
//            FireBaseInstance.getInfoUser(senderId) { user ->
//                user.keyAuth = senderId
//                sendNotification(it.title, it.body, senderId, user)
//            }
//        }
    }

    @SuppressLint("ServiceCast")
    private fun sendNotification(title: String?, messageBody: String?, senderId: String, friend:User) {
        val channelId = Random().nextInt()
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(OBJECT_FRIEND, friend)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        // for replies on notification
        val remoteInput = RemoteInput.Builder(KEY_REPLY_TEXT)
            .setLabel("Reply")
            .build()

        // Create a PendingIntent for the reply action
        val replyIntent = Intent(this, NotificationReply::class.java)
        replyIntent.putExtra(SENDER_ID, senderId)

        val replyPendingIntent = PendingIntent.getBroadcast(this, 0, replyIntent, PendingIntent.FLAG_MUTABLE)

        // Create a NotificationCompat.Action object for the reply action
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_reply,
            "Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.title_app))
            .setSmallIcon(R.drawable.ic_message)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .addAction(replyAction)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.title_app),
                "Channel human readable title",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        shared.saveChannelId(channelId)
        notificationManager.notify(channelId, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        const val KEY_REPLY_TEXT = "KEY_REPLY_TEXT"
        const val SENDER_ID = "SENDER_ID"
        const val OBJECT_FRIEND = "OBJECT_FRIEND"
    }
}
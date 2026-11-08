package com.example.messageapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.messageapp.R
import com.example.messageapp.broadcast.CallActionReceiver
import com.example.messageapp.call.CallActivity

/** Tạo channel và notification cho cuộc gọi (đến + đang gọi). */
object CallNotifier {
    private const val CHANNEL_INCOMING = "call_incoming"
    private const val CHANNEL_ONGOING = "call_ongoing"
    const val NOTIF_ID_INCOMING = 7001
    const val NOTIF_ID_ONGOING = 7002

    private fun manager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = manager(context)
        if (nm.getNotificationChannel(CHANNEL_INCOMING) == null) {
            val channel = NotificationChannel(
                CHANNEL_INCOMING,
                context.getString(R.string.call_channel_incoming),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 600, 800, 600)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
        if (nm.getNotificationChannel(CHANNEL_ONGOING) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ONGOING,
                    context.getString(R.string.call_channel_ongoing),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
    }

    /** Notification cuộc gọi đến: full-screen intent + nút Nghe / Từ chối + chuông lặp. */
    fun showIncoming(context: Context, callId: String, callerName: String, isVideo: Boolean) {
        ensureChannels(context)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val open = PendingIntent.getActivity(
            context, 1, CallActivity.intentForIncoming(context, callId), flags
        )
        val accept = PendingIntent.getActivity(
            context, 2,
            CallActivity.intentForIncoming(context, callId).putExtra(CallActivity.EXTRA_AUTO_ACCEPT, true),
            flags,
        )
        val reject = PendingIntent.getBroadcast(
            context, 3,
            Intent(context, CallActionReceiver::class.java)
                .setAction(CallActionReceiver.ACTION_REJECT)
                .putExtra(CallActionReceiver.EXTRA_CALL_ID, callId),
            flags,
        )

        val text = context.getString(
            if (isVideo) R.string.call_status_incoming_video else R.string.call_status_incoming_voice
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_INCOMING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(CALL_NOTIFICATION_TIMEOUT_MS)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .addAction(0, context.getString(R.string.call_cd_reject), reject)
            .addAction(0, context.getString(R.string.call_cd_accept), accept)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                .setVibrate(longArrayOf(0, 800, 600, 800, 600))
        }
        val notification = builder.build().apply {
            this.flags = this.flags or Notification.FLAG_INSISTENT // lặp chuông cho tới khi xử lý
        }
        manager(context).notify(NOTIF_ID_INCOMING, notification)
    }

    fun cancelIncoming(context: Context) {
        manager(context).cancel(NOTIF_ID_INCOMING)
    }

    /** Notification foreground cho cuộc gọi đang diễn ra. */
    fun buildOngoing(context: Context, peerName: String, statusText: String): Notification {
        ensureChannels(context)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(
            context, 4,
            Intent(context, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flags,
        )
        val end = PendingIntent.getBroadcast(
            context, 5,
            Intent(context, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_END),
            flags,
        )
        return NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(peerName)
            .setContentText(statusText)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.call_cd_end), end)
            .build()
    }

    private const val CALL_NOTIFICATION_TIMEOUT_MS = 45_000L
}

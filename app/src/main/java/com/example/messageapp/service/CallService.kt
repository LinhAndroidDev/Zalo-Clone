package com.example.messageapp.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.messageapp.R
import com.example.messageapp.call.CallManager
import com.example.messageapp.call.CallManager.Phase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service giữ cuộc gọi (micro/camera) khi app ra nền.
 * Do CallManager khởi động khi bắt đầu gọi/nghe máy và tự dừng khi cuộc gọi về IDLE.
 */
@AndroidEntryPoint
class CallService : Service() {

    @Inject lateinit var callManager: CallManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = callManager.state.value
        if (state.phase == Phase.IDLE) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Phải gọi startForeground ngay (trong ~5s sau khi được start).
        if (!startForegroundSafely(state)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (observeJob == null) {
            observeJob = scope.launch {
                callManager.state.collect { s ->
                    if (s.phase == Phase.IDLE) {
                        stopSelf()
                    } else if (s.phase != Phase.ENDED) {
                        startForegroundSafely(s)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundSafely(state: CallManager.State): Boolean = try {
        val call = state.call
        val peerName = (if (state.isOutgoing) call?.calleeName else call?.callerName).orEmpty()
        val status = getString(
            when (state.phase) {
                Phase.OUTGOING -> R.string.call_status_calling
                Phase.CONNECTING -> R.string.call_status_connecting
                else -> R.string.call_notification_ongoing
            }
        )
        ServiceCompat.startForeground(
            this,
            CallNotifier.NOTIF_ID_ONGOING,
            CallNotifier.buildOngoing(this, peerName, status),
            foregroundType(state.isVideo),
        )
        true
    } catch (e: Exception) {
        // Ví dụ: Android 12+ chặn start FGS từ nền, hoặc thiếu quyền cho type micro/camera.
        Log.e(TAG, "startForeground failed", e)
        false
    }

    /** Chỉ khai báo type mà quyền runtime đã được cấp (Android 14 yêu cầu). */
    private fun foregroundType(video: Boolean): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        var type = 0
        if (granted(Manifest.permission.RECORD_AUDIO)) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        if (video && granted(Manifest.permission.CAMERA)) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        return type
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CallService"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, CallService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "cannot start CallService", e)
            }
        }
    }
}

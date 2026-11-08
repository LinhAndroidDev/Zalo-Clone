package com.example.messageapp.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.messageapp.call.CallManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Xử lý nút "Từ chối" (notification cuộc gọi đến) và "Kết thúc" (notification đang gọi). */
@AndroidEntryPoint
class CallActionReceiver : BroadcastReceiver() {

    @Inject lateinit var callManager: CallManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REJECT -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID).orEmpty()
                val pending = goAsync()
                callManager.rejectIncoming(callId) { pending.finish() }
            }
            ACTION_END -> callManager.endCall()
        }
    }

    companion object {
        const val ACTION_REJECT = "com.example.messageapp.call.REJECT"
        const val ACTION_END = "com.example.messageapp.call.END"
        const val EXTRA_CALL_ID = "extra_call_id"
    }
}

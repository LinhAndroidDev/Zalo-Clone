package com.example.messageapp.call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.messageapp.R
import com.example.messageapp.call.CallManager.EndReason
import com.example.messageapp.call.CallManager.Phase
import com.example.messageapp.databinding.ActivityCallBinding
import com.example.messageapp.domain.model.CallType
import com.example.messageapp.permission.AppPermission
import com.example.messageapp.permission.PermissionRequester
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.webrtc.WebRtcEgl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack
import javax.inject.Inject

/**
 * Một màn hình cho cả gọi đi, gọi đến và đang gọi. Chỉ hiển thị [CallManager.state];
 * mọi logic nằm ở CallManager.
 *
 * Gọi đi: CallManager.startCall(...) rồi [start]. Gọi đến: truyền [EXTRA_CALL_ID].
 */
@AndroidEntryPoint
class CallActivity : AppCompatActivity() {

    @Inject
    lateinit var callManager: CallManager

    private val binding by lazy { ActivityCallBinding.inflate(layoutInflater) }
    private val permissionRequester = PermissionRequester(this)

    private var seenActive = false
    private var remoteTrack: VideoTrack? = null
    private var localTrack: VideoTrack? = null
    private var renderersReleased = false
    private var autoAccept = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Phím âm lượng điều chỉnh âm lượng CUỘC GỌI (không phải media) trên màn hình này.
        volumeControlStream = android.media.AudioManager.STREAM_VOICE_CALL
        setContentView(binding.root)

        initRenderers()
        initClicks()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            // Chặn back khi đang gọi để tránh mất màn hình điều khiển; dùng nút kết thúc.
            override fun handleOnBackPressed() = Unit
        })

        handleIntent(intent)
        observeState()
        observeVideoTracks()
        observeDuration()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val callId = intent.getStringExtra(EXTRA_CALL_ID)
        if (intent.getBooleanExtra(EXTRA_AUTO_ACCEPT, false)) autoAccept = true
        if (!callId.isNullOrBlank()) callManager.onIncomingCall(callId)
        // Nếu không có callId (gọi đi) thì CallManager đã ở trạng thái OUTGOING.
    }

    private fun initRenderers() {
        val egl = WebRtcEgl.eglBase.eglBaseContext
        binding.remoteVideo.init(egl, null)
        binding.remoteVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        binding.localVideo.init(egl, null)
        binding.localVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        binding.localVideo.setMirror(true)
        binding.localVideo.setZOrderMediaOverlay(true)
    }

    private fun initClicks() = with(binding) {
        btnEnd.setOnClickListener { callManager.endCall() }
        btnReject.setOnClickListener { callManager.rejectCall() }
        btnAccept.setOnClickListener { acceptWithPermission() }
        btnMic.setOnClickListener { callManager.toggleMic() }
        btnCamera.setOnClickListener { callManager.toggleCamera() }
        btnFlip.setOnClickListener { callManager.switchCamera() }
        btnSpeaker.setOnClickListener { callManager.toggleSpeaker() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Nếu sau vài giây vẫn không có cuộc gọi nào (ví dụ push cũ) thì đóng.
                launch {
                    delay(NO_CALL_TIMEOUT_MS)
                    if (!seenActive) finish()
                }
                callManager.state.collect { render(it) }
            }
        }
    }

    private fun render(state: CallManager.State) = with(binding) {
        if (state.phase == Phase.IDLE) {
            if (seenActive) finish()
            return@with
        }
        seenActive = true
        if (autoAccept && state.phase == Phase.INCOMING) {
            autoAccept = false // người dùng bấm "Nghe" trên notification
            acceptWithPermission()
        }
        val call = state.call
        val peerName = if (state.isOutgoing) call?.calleeName else call?.callerName
        val peerAvatar = if (state.isOutgoing) call?.calleeAvatar else call?.callerAvatar
        tvName.text = peerName.orEmpty()
        tvVideoName.text = peerName.orEmpty()
        if (!peerAvatar.isNullOrBlank()) loadImg(peerAvatar, imgAvatar, R.drawable.bg_grey_equal)

        val status = statusText(state)
        tvStatus.text = status
        if (state.phase != Phase.CONNECTED) tvVideoStatus.text = status

        layoutIncomingControls.visibility = visibleIf(state.phase == Phase.INCOMING)
        layoutCallControls.visibility = visibleIf(state.phase != Phase.INCOMING && state.phase != Phase.ENDED)
        btnCamera.visibility = visibleIf(state.isVideo)
        btnFlip.visibility = visibleIf(state.isVideo)

        btnMic.setImageResource(if (state.isMicOn) R.drawable.ic_call_mic else R.drawable.ic_call_mic_off)
        btnMic.setBackgroundResource(if (state.isMicOn) R.drawable.bg_call_control else R.drawable.bg_call_control_off)
        btnCamera.setImageResource(if (state.isCameraOn) R.drawable.ic_call_camera else R.drawable.ic_call_camera_off)
        btnCamera.setBackgroundResource(if (state.isCameraOn) R.drawable.bg_call_control else R.drawable.bg_call_control_off)
        btnSpeaker.setBackgroundResource(if (state.isSpeakerOn) R.drawable.bg_call_control_off else R.drawable.bg_call_control)

        updateVideoVisibility(state)
    }

    private fun updateVideoVisibility(state: CallManager.State) = with(binding) {
        val remoteShown = state.isVideo && remoteTrack != null &&
            (state.phase == Phase.CONNECTING || state.phase == Phase.CONNECTED)
        remoteVideo.visibility = visibleIf(remoteShown)
        layoutVideoHeader.visibility = visibleIf(remoteShown)
        layoutInfo.visibility = visibleIf(!remoteShown)
        localVideo.visibility = visibleIf(state.isVideo && state.isCameraOn && localTrack != null)
    }

    private fun observeVideoTracks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    callManager.remoteVideoTrack.collect { track ->
                        remoteTrack?.removeSink(binding.remoteVideo)
                        remoteTrack = track
                        if (!renderersReleased) track?.addSink(binding.remoteVideo)
                        updateVideoVisibility(callManager.state.value)
                    }
                }
                launch {
                    callManager.localVideoTrack.collect { track ->
                        localTrack?.removeSink(binding.localVideo)
                        localTrack = track
                        if (!renderersReleased) track?.addSink(binding.localVideo)
                        updateVideoVisibility(callManager.state.value)
                    }
                }
            }
        }
    }

    private fun observeDuration() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val s = callManager.state.value
                    if (s.phase == Phase.CONNECTED) {
                        val text = formatDuration(SystemClock.elapsedRealtime() - s.connectedAtElapsed)
                        binding.tvStatus.text = text
                        binding.tvVideoStatus.text = text
                    }
                    delay(500)
                }
            }
        }
    }

    private fun acceptWithPermission() {
        val permission =
            if (callManager.state.value.isVideo) AppPermission.VIDEO_CALL else AppPermission.VOICE_CALL
        permissionRequester.request(permission) { callManager.acceptCall() }
    }

    private fun statusText(state: CallManager.State): String = when (state.phase) {
        Phase.OUTGOING -> getString(R.string.call_status_calling)
        Phase.INCOMING -> getString(
            if (state.isVideo) R.string.call_status_incoming_video else R.string.call_status_incoming_voice
        )
        Phase.CONNECTING -> getString(R.string.call_status_connecting)
        Phase.CONNECTED -> formatDuration(SystemClock.elapsedRealtime() - state.connectedAtElapsed)
        Phase.ENDED -> getString(endReasonRes(state.endReason))
        Phase.IDLE -> ""
    }

    private fun endReasonRes(reason: EndReason?): Int = when (reason) {
        EndReason.LOCAL_HANGUP -> R.string.call_end_local
        EndReason.REMOTE_HANGUP -> R.string.call_end_remote
        EndReason.REJECTED -> R.string.call_end_rejected
        EndReason.CANCELLED -> R.string.call_end_cancelled
        EndReason.MISSED -> R.string.call_end_missed
        EndReason.BUSY -> R.string.call_end_busy
        EndReason.NOT_FRIEND -> R.string.call_end_not_friend
        EndReason.CONNECTION_LOST -> R.string.call_end_connection_lost
        EndReason.FAILED, null -> R.string.call_end_failed
    }

    private fun formatDuration(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return "%02d:%02d".format(total / 60, total % 60)
    }

    private fun visibleIf(condition: Boolean) = if (condition) View.VISIBLE else View.GONE

    override fun onDestroy() {
        remoteTrack?.removeSink(binding.remoteVideo)
        localTrack?.removeSink(binding.localVideo)
        renderersReleased = true
        binding.remoteVideo.release()
        binding.localVideo.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_AUTO_ACCEPT = "extra_auto_accept"
        private const val NO_CALL_TIMEOUT_MS = 6_000L

        /** Mở màn hình cho cuộc gọi đi (CallManager.startCall đã được gọi trước đó). */
        fun start(context: Context) {
            context.startActivity(
                Intent(context, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        /** Mở màn hình cho cuộc gọi đến. */
        fun intentForIncoming(context: Context, callId: String): Intent =
            Intent(context, CallActivity::class.java)
                .putExtra(EXTRA_CALL_ID, callId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

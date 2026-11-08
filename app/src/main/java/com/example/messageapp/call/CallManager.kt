package com.example.messageapp.call

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.example.messageapp.domain.model.Call
import com.example.messageapp.domain.model.CallIceCandidate
import com.example.messageapp.domain.model.CallStatus
import com.example.messageapp.domain.model.CallType
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.call.AddIceCandidateUseCase
import com.example.messageapp.domain.usecase.call.AnswerCallUseCase
import com.example.messageapp.domain.usecase.call.CallException
import com.example.messageapp.domain.usecase.call.GetCallUseCase
import com.example.messageapp.domain.usecase.call.ObserveCallUseCase
import com.example.messageapp.domain.usecase.call.ObserveIceCandidatesUseCase
import com.example.messageapp.domain.usecase.call.StartCallUseCase
import com.example.messageapp.domain.usecase.call.UpdateCallStatusUseCase
import com.example.messageapp.webrtc.CallConfig
import com.example.messageapp.webrtc.WebRtcClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import org.webrtc.VideoTrack
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Điều phối MỘT cuộc gọi tại một thời điểm: signaling (use case) + WebRTC + âm thanh.
 * UI (CallActivity, notification) chỉ quan sát [state] và gọi các hàm công khai.
 * Mọi hàm công khai gọi từ main thread.
 */
@Singleton
class CallManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val startCallUseCase: StartCallUseCase,
    private val observeCallUseCase: ObserveCallUseCase,
    private val getCallUseCase: GetCallUseCase,
    private val answerCallUseCase: AnswerCallUseCase,
    private val updateCallStatusUseCase: UpdateCallStatusUseCase,
    private val addIceCandidateUseCase: AddIceCandidateUseCase,
    private val observeIceCandidatesUseCase: ObserveIceCandidatesUseCase,
) {
    enum class Phase { IDLE, OUTGOING, INCOMING, CONNECTING, CONNECTED, ENDED }

    enum class EndReason {
        LOCAL_HANGUP, REMOTE_HANGUP, REJECTED, CANCELLED, MISSED, BUSY,
        NOT_FRIEND, FAILED, CONNECTION_LOST,
    }

    data class State(
        val phase: Phase = Phase.IDLE,
        val call: Call? = null,
        /** true: mình là người gọi; false: mình là người nhận. */
        val isOutgoing: Boolean = false,
        val isMicOn: Boolean = true,
        val isCameraOn: Boolean = false,
        val isSpeakerOn: Boolean = false,
        /** SystemClock.elapsedRealtime() lúc kết nối thành công, để UI tính thời lượng. */
        val connectedAtElapsed: Long = 0L,
        val endReason: EndReason? = null,
    ) {
        val isVideo: Boolean get() = call?.type == CallType.VIDEO
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val clientFlow = MutableStateFlow<WebRtcClient?>(null)

    /** Video của mình / của đối phương, để UI gắn vào SurfaceViewRenderer. */
    val localVideoTrack: StateFlow<VideoTrack?> = clientFlow
        .flatMapLatest { it?.localVideoTrack ?: flowOf(null) }
        .stateIn(scope, SharingStarted.Eagerly, null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = clientFlow
        .flatMapLatest { it?.remoteVideoTrack ?: flowOf(null) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val audio = CallAudioManager(context)

    private var client: WebRtcClient? = null
    private var activeCallId: String? = null
    private var isCaller = false
    private var answerApplied = false

    private var sessionJob: Job? = null
    private var callObserverJob: Job? = null
    private var candidateObserverJob: Job? = null
    private var timeoutJob: Job? = null
    private var disconnectJob: Job? = null
    private val pendingLocalCandidates = mutableListOf<CallIceCandidate>()

    // ------------------------------------------------------------------ Public API

    /** Gọi đi. Cần đã có quyền (AppPermission.VOICE_CALL / VIDEO_CALL) trước khi gọi hàm này. */
    fun startCall(calleeId: String, calleeName: String, calleeAvatar: String, type: CallType) {
        if (_state.value.phase != Phase.IDLE) {
            Log.w(TAG, "startCall ignored, phase=${_state.value.phase}")
            return
        }
        val video = type == CallType.VIDEO
        isCaller = true
        answerApplied = false
        _state.value = State(
            phase = Phase.OUTGOING,
            isOutgoing = true,
            call = Call(calleeId = calleeId, calleeName = calleeName, calleeAvatar = calleeAvatar, type = type),
            isCameraOn = video,
            isSpeakerOn = video,
        )
        sessionJob = scope.launch {
            try {
                val c = newClient(video)
                audio.start(speakerOn = video)
                val offer = c.createOffer()
                val call = startCallUseCase(calleeId, calleeName, calleeAvatar, type, offer).getOrElse {
                    Log.e(TAG, "startCall failed", it)
                    finish(reasonOf(it))
                    return@launch
                }
                if (isFinished()) {
                    // Người dùng đã hủy trong lúc đang tạo cuộc gọi.
                    updateCallStatusUseCase(call.callId, CallStatus.CANCELLED)
                    return@launch
                }
                activeCallId = call.callId
                _state.value = _state.value.copy(call = call)
                flushLocalCandidates()
                observeCall(call.callId)
                observeRemoteCandidates(call.callId, fromCaller = false)
                startTimeout(CallConfig.RINGING_TIMEOUT_MS) { finish(EndReason.MISSED, CallStatus.MISSED) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "startCall error", e)
                finish(EndReason.FAILED)
            }
        }
    }

    /** Có cuộc gọi đến (từ push hoặc từ màn hình cuộc gọi đến). */
    fun onIncomingCall(callId: String) {
        if (callId.isBlank() || activeCallId == callId) return
        scope.launch {
            val call = getCallUseCase(callId) ?: return@launch
            val myId = sessionRepository.getAuth()
            val stale = System.currentTimeMillis() - call.createdAt > CallConfig.INCOMING_STALE_MS
            if (call.calleeId != myId || call.status != CallStatus.RINGING || stale) return@launch
            if (_state.value.phase != Phase.IDLE) {
                updateCallStatusUseCase(callId, CallStatus.BUSY)
                return@launch
            }
            val video = call.type == CallType.VIDEO
            isCaller = false
            answerApplied = false
            activeCallId = callId
            _state.value = State(
                phase = Phase.INCOMING,
                call = call,
                isCameraOn = video,
                isSpeakerOn = video,
            )
            observeCall(callId)
            startTimeout(CallConfig.RINGING_TIMEOUT_MS + 10_000L) { finish(EndReason.MISSED) }
        }
    }

    /** Callee nghe máy. Cần đã có quyền trước khi gọi. */
    fun acceptCall() {
        val s = _state.value
        val call = s.call
        if (s.phase != Phase.INCOMING || call == null) return
        val offer = call.offer
        if (offer == null) {
            finish(EndReason.FAILED)
            return
        }
        timeoutJob?.cancel()
        _state.value = s.copy(phase = Phase.CONNECTING)
        sessionJob = scope.launch {
            try {
                val c = newClient(call.type == CallType.VIDEO)
                audio.start(speakerOn = s.isSpeakerOn)
                val answer = c.createAnswer(offer)
                answerCallUseCase(call.callId, answer).getOrElse {
                    Log.e(TAG, "answerCall failed", it)
                    finish(EndReason.FAILED)
                    return@launch
                }
                flushLocalCandidates()
                observeRemoteCandidates(call.callId, fromCaller = true)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "acceptCall error", e)
                finish(EndReason.FAILED, CallStatus.ENDED)
            }
        }
    }

    /** Callee từ chối. */
    fun rejectCall() {
        if (_state.value.phase != Phase.INCOMING) return
        finish(EndReason.REJECTED, CallStatus.REJECTED)
    }

    /** Kết thúc/hủy cuộc gọi hiện tại tùy giai đoạn. */
    fun endCall() {
        when (_state.value.phase) {
            Phase.OUTGOING -> finish(EndReason.CANCELLED, CallStatus.CANCELLED)
            Phase.INCOMING -> finish(EndReason.REJECTED, CallStatus.REJECTED)
            Phase.CONNECTING, Phase.CONNECTED -> finish(EndReason.LOCAL_HANGUP, CallStatus.ENDED)
            else -> Unit
        }
    }

    fun toggleMic() {
        val s = _state.value
        val on = !s.isMicOn
        client?.setMicEnabled(on)
        _state.value = s.copy(isMicOn = on)
    }

    fun toggleCamera() {
        val s = _state.value
        if (!s.isVideo) return
        val on = !s.isCameraOn
        client?.setCameraEnabled(on)
        _state.value = s.copy(isCameraOn = on)
    }

    fun toggleSpeaker() {
        val s = _state.value
        val on = !s.isSpeakerOn
        audio.setSpeaker(on)
        _state.value = s.copy(isSpeakerOn = on)
    }

    fun switchCamera() {
        if (_state.value.isVideo) client?.switchCamera()
    }

    // ------------------------------------------------------------------ Internals

    private fun newClient(withVideo: Boolean): WebRtcClient {
        val c = WebRtcClient(context, clientListener)
        c.start(withVideo)
        client = c
        clientFlow.value = c
        return c
    }

    private val clientListener = object : WebRtcClient.Listener {
        override fun onLocalIceCandidate(candidate: CallIceCandidate) {
            scope.launch { sendLocalCandidate(candidate) }
        }

        override fun onConnectionStateChanged(state: PeerConnection.PeerConnectionState) {
            scope.launch { handleConnectionState(state) }
        }
    }

    private fun handleConnectionState(state: PeerConnection.PeerConnectionState) {
        val s = _state.value
        if (s.phase == Phase.IDLE || s.phase == Phase.ENDED) return
        when (state) {
            PeerConnection.PeerConnectionState.CONNECTED -> {
                disconnectJob?.cancel()
                timeoutJob?.cancel()
                if (s.phase != Phase.CONNECTED) {
                    _state.value = s.copy(phase = Phase.CONNECTED, connectedAtElapsed = SystemClock.elapsedRealtime())
                }
            }
            PeerConnection.PeerConnectionState.DISCONNECTED -> {
                disconnectJob?.cancel()
                disconnectJob = scope.launch {
                    delay(CallConfig.DISCONNECT_GRACE_MS)
                    finish(EndReason.CONNECTION_LOST, CallStatus.ENDED)
                }
            }
            PeerConnection.PeerConnectionState.FAILED ->
                finish(EndReason.CONNECTION_LOST, CallStatus.ENDED)
            else -> Unit
        }
    }

    private suspend fun sendLocalCandidate(candidate: CallIceCandidate) {
        val id = activeCallId
        if (id == null) {
            pendingLocalCandidates.add(candidate) // caller: chưa có callId, gửi sau
        } else {
            addIceCandidateUseCase(id, isCaller, candidate)
        }
    }

    private suspend fun flushLocalCandidates() {
        val id = activeCallId ?: return
        val toSend = pendingLocalCandidates.toList()
        pendingLocalCandidates.clear()
        toSend.forEach { addIceCandidateUseCase(id, isCaller, it) }
    }

    private fun observeCall(callId: String) {
        callObserverJob?.cancel()
        callObserverJob = scope.launch {
            observeCallUseCase(callId).collect { call ->
                if (call == null) return@collect
                val s = _state.value
                if (s.phase == Phase.IDLE || s.phase == Phase.ENDED) return@collect
                _state.value = s.copy(call = call)
                when (call.status) {
                    CallStatus.ACCEPTED -> {
                        val answer = call.answer
                        if (isCaller && !answerApplied && answer != null) {
                            answerApplied = true
                            timeoutJob?.cancel()
                            _state.value = _state.value.copy(phase = Phase.CONNECTING)
                            runCatching { client?.setRemoteAnswer(answer) }
                                .onFailure {
                                    Log.e(TAG, "setRemoteAnswer failed", it)
                                    finish(EndReason.FAILED, CallStatus.ENDED)
                                }
                        }
                    }
                    CallStatus.REJECTED -> if (isCaller) finish(EndReason.REJECTED)
                    CallStatus.BUSY -> finish(EndReason.BUSY)
                    CallStatus.CANCELLED -> finish(EndReason.CANCELLED)
                    CallStatus.MISSED -> finish(EndReason.MISSED)
                    CallStatus.ENDED -> finish(EndReason.REMOTE_HANGUP)
                    CallStatus.RINGING -> Unit
                }
            }
        }
    }

    private fun observeRemoteCandidates(callId: String, fromCaller: Boolean) {
        candidateObserverJob?.cancel()
        candidateObserverJob = scope.launch {
            observeIceCandidatesUseCase(callId, fromCaller).collect { client?.addRemoteIceCandidate(it) }
        }
    }

    private fun startTimeout(ms: Long, onTimeout: () -> Unit) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(ms)
            onTimeout()
        }
    }

    private fun isFinished(): Boolean = _state.value.phase.let { it == Phase.IDLE || it == Phase.ENDED }

    /**
     * Kết thúc cuộc gọi (idempotent): dừng mọi job, giải phóng WebRTC/âm thanh, chuyển ENDED,
     * ghi trạng thái lên Firestore nếu [writeStatus] != null, rồi về IDLE sau một lúc.
     */
    private fun finish(reason: EndReason, writeStatus: CallStatus? = null) {
        val s = _state.value
        if (s.phase == Phase.IDLE || s.phase == Phase.ENDED) return
        val id = activeCallId
        Log.d(TAG, "finish reason=$reason status=$writeStatus phase=${s.phase}")

        timeoutJob?.cancel()
        disconnectJob?.cancel()
        callObserverJob?.cancel()
        candidateObserverJob?.cancel()
        sessionJob?.cancel()
        clientFlow.value = null
        client?.close()
        client = null
        audio.stop()
        pendingLocalCandidates.clear()

        _state.value = s.copy(phase = Phase.ENDED, endReason = reason)

        if (writeStatus != null && id != null) {
            scope.launch { updateCallStatusUseCase(id, writeStatus) }
        }
        scope.launch {
            delay(CallConfig.END_STATE_HOLD_MS)
            if (_state.value.phase == Phase.ENDED) {
                activeCallId = null
                _state.value = State()
            }
        }
    }

    private fun reasonOf(error: Throwable): EndReason = when ((error as? CallException)?.reason) {
        CallException.Reason.NOT_FRIEND -> EndReason.NOT_FRIEND
        CallException.Reason.CALLEE_BUSY -> EndReason.BUSY
        else -> EndReason.FAILED
    }

    companion object {
        private const val TAG = "CallManager"
    }
}

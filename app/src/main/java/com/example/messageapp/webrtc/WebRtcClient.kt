package com.example.messageapp.webrtc

import android.content.Context
import android.util.Log
import com.example.messageapp.domain.model.CallIceCandidate
import com.example.messageapp.domain.model.CallSessionDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** EGL dùng chung cho cả process: renderer (UI) và encoder/decoder phải cùng context. */
object WebRtcEgl {
    val eglBase: EglBase by lazy { EglBase.create() }
}

/**
 * Bọc một PeerConnection cho đúng MỘT cuộc gọi. Tạo mới cho mỗi cuộc gọi, gọi [close] khi xong.
 * Không biết gì về Firestore: signaling do CallManager lo.
 */
class WebRtcClient(
    context: Context,
    private val listener: Listener,
) {
    interface Listener {
        fun onLocalIceCandidate(candidate: CallIceCandidate)
        fun onConnectionStateChanged(state: PeerConnection.PeerConnectionState)
    }

    private val appContext = context.applicationContext
    private val eglBase = WebRtcEgl.eglBase

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()
    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var capturer: CameraVideoCapturer? = null
    private var surfaceHelper: SurfaceTextureHelper? = null

    private var remoteDescriptionSet = false
    private val pendingRemoteCandidates = mutableListOf<IceCandidate>()
    private var closed = false

    /** Tạo factory + peer connection + track âm thanh (và video nếu [withVideo]). */
    fun start(withVideo: Boolean) {
        check(peerConnection == null) { "WebRtcClient already started" }
        initFactoryGlobally()
        // Dùng khử echo/khử ồn bằng phần mềm của WebRTC (AEC3) thay cho phần cứng: AEC phần cứng trên
        // nhiều máy hoạt động kém (echo, tiếng chói) và WebRTC sẽ tự tắt AEC phần mềm nếu AEC phần
        // cứng được bật.
        val audioModule = JavaAudioDeviceModule.builder(appContext)
            .setUseHardwareAcousticEchoCanceler(false)
            .setUseHardwareNoiseSuppressor(false)
            .createAudioDeviceModule()
        val f = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioModule)
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
        // audioModule được factory giữ tham chiếu; giải phóng cùng factory.
        factory = f

        val config = PeerConnection.RTCConfiguration(CallConfig.iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        val pc = f.createPeerConnection(config, observer)
            ?: throw IllegalStateException("Cannot create PeerConnection")
        peerConnection = pc

        val audioSource = f.createAudioSource(MediaConstraints())
        audioTrack = f.createAudioTrack(AUDIO_TRACK_ID, audioSource).also {
            it.setEnabled(true)
            pc.addTrack(it, listOf(STREAM_ID))
        }

        if (withVideo) startLocalVideo(f, pc)
    }

    private fun startLocalVideo(f: PeerConnectionFactory, pc: PeerConnection) {
        val enumerator = Camera2Enumerator(appContext)
        val deviceName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: throw IllegalStateException("No camera available")
        val cameraCapturer = enumerator.createCapturer(deviceName, null)
            ?: throw IllegalStateException("Cannot create camera capturer")
        val helper = SurfaceTextureHelper.create("CallCaptureThread", eglBase.eglBaseContext)
        val source = f.createVideoSource(cameraCapturer.isScreencast)
        cameraCapturer.initialize(helper, appContext, source.capturerObserver)
        cameraCapturer.startCapture(CallConfig.VIDEO_WIDTH, CallConfig.VIDEO_HEIGHT, CallConfig.VIDEO_FPS)

        val track = f.createVideoTrack(VIDEO_TRACK_ID, source).also {
            it.setEnabled(true)
            pc.addTrack(it, listOf(STREAM_ID))
        }
        capturer = cameraCapturer
        surfaceHelper = helper
        videoSource = source
        _localVideoTrack.value = track
    }

    // ---- SDP ----

    suspend fun createOffer(): CallSessionDescription {
        val pc = requirePc()
        val offer = pc.createSdp { observer -> pc.createOffer(observer, MediaConstraints()) }
        pc.setLocal(offer)
        return offer.toDomain()
    }

    /** Nhận offer của caller, trả về answer (đã set local). */
    suspend fun createAnswer(offer: CallSessionDescription): CallSessionDescription {
        val pc = requirePc()
        setRemoteDescription(offer)
        val answer = pc.createSdp { observer -> pc.createAnswer(observer, MediaConstraints()) }
        pc.setLocal(answer)
        return answer.toDomain()
    }

    suspend fun setRemoteAnswer(answer: CallSessionDescription) = setRemoteDescription(answer)

    private suspend fun setRemoteDescription(description: CallSessionDescription) {
        val pc = requirePc()
        pc.setRemote(description.toWebRtc())
        remoteDescriptionSet = true
        pendingRemoteCandidates.forEach { pc.addIceCandidate(it) }
        pendingRemoteCandidates.clear()
    }

    /** Candidate đến trước remote description sẽ được giữ lại và thêm sau. */
    fun addRemoteIceCandidate(candidate: CallIceCandidate) {
        val ice = IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate)
        val pc = peerConnection ?: return
        if (remoteDescriptionSet) pc.addIceCandidate(ice) else pendingRemoteCandidates.add(ice)
    }

    // ---- Controls ----

    fun setMicEnabled(enabled: Boolean) {
        audioTrack?.setEnabled(enabled)
    }

    fun setCameraEnabled(enabled: Boolean) {
        _localVideoTrack.value?.setEnabled(enabled)
    }

    fun switchCamera() {
        capturer?.switchCamera(null)
    }

    // ---- Lifecycle ----

    fun close() {
        if (closed) return
        closed = true
        runCatching { capturer?.stopCapture() }
        runCatching { capturer?.dispose() }
        capturer = null
        _localVideoTrack.value = null
        _remoteVideoTrack.value = null
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        peerConnection = null
        runCatching { audioTrack?.dispose() }
        audioTrack = null
        runCatching { videoSource?.dispose() }
        videoSource = null
        runCatching { surfaceHelper?.dispose() }
        surfaceHelper = null
        runCatching { factory?.dispose() }
        factory = null
        pendingRemoteCandidates.clear()
    }

    private fun requirePc(): PeerConnection = peerConnection ?: throw IllegalStateException("WebRtcClient not started")

    private val observer = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            listener.onLocalIceCandidate(
                CallIceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
            )
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
            Log.d(TAG, "connection state: $newState")
            listener.onConnectionStateChanged(newState)
        }

        override fun onTrack(transceiver: RtpTransceiver) {
            val track = transceiver.receiver.track()
            if (track is VideoTrack) _remoteVideoTrack.value = track
        }

        override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) = Unit
        override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            Log.d(TAG, "ice state: $state")
        }
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
        override fun onAddStream(stream: MediaStream?) = Unit
        override fun onRemoveStream(stream: MediaStream?) = Unit
        override fun onDataChannel(channel: DataChannel?) = Unit
        override fun onRenegotiationNeeded() = Unit
    }

    companion object {
        private const val TAG = "WebRtcClient"
        private const val AUDIO_TRACK_ID = "call_audio"
        private const val VIDEO_TRACK_ID = "call_video"
        private const val STREAM_ID = "call_stream"
        private var factoryInitialized = false

        @Synchronized
        private fun WebRtcClient.initFactoryGlobally() {
            if (factoryInitialized) return
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(appContext).createInitializationOptions()
            )
            factoryInitialized = true
        }
    }
}

// ---- Helpers chuyển callback của WebRTC sang suspend ----

private fun SessionDescription.toDomain() = CallSessionDescription(
    type = type.canonicalForm(),
    sdp = description,
)

private fun CallSessionDescription.toWebRtc() = SessionDescription(
    SessionDescription.Type.fromCanonicalForm(type),
    sdp,
)

private suspend fun PeerConnection.createSdp(
    create: (SdpObserver) -> Unit,
): SessionDescription = suspendCancellableCoroutine { cont ->
    create(object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) = cont.resume(sdp)
        override fun onCreateFailure(error: String?) =
            cont.resumeWithException(IllegalStateException("createSdp failed: $error"))
        override fun onSetSuccess() = Unit
        override fun onSetFailure(error: String?) = Unit
    })
}

private suspend fun PeerConnection.setLocal(sdp: SessionDescription) = setSdp { setLocalDescription(it, sdp) }

private suspend fun PeerConnection.setRemote(sdp: SessionDescription) = setSdp { setRemoteDescription(it, sdp) }

private suspend fun setSdp(set: (SdpObserver) -> Unit) = suspendCancellableCoroutine<Unit> { cont ->
    set(object : SdpObserver {
        override fun onSetSuccess() = cont.resume(Unit)
        override fun onSetFailure(error: String?) =
            cont.resumeWithException(IllegalStateException("setSdp failed: $error"))
        override fun onCreateSuccess(sdp: SessionDescription?) = Unit
        override fun onCreateFailure(error: String?) = Unit
    })
}

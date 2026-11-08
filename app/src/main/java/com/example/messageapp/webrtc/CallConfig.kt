package com.example.messageapp.webrtc

import org.webrtc.PeerConnection

/** Cấu hình WebRTC tập trung một chỗ. Thêm TURN server vào [iceServers] khi cần. */
object CallConfig {
    const val RINGING_TIMEOUT_MS = 30_000L
    const val INCOMING_STALE_MS = 60_000L
    const val DISCONNECT_GRACE_MS = 8_000L
    const val END_STATE_HOLD_MS = 1_500L

    const val VIDEO_WIDTH = 1280
    const val VIDEO_HEIGHT = 720
    const val VIDEO_FPS = 30

    val iceServers: List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        // TODO: thêm TURN, ví dụ:
        // PeerConnection.IceServer.builder("turn:host:3478").setUsername("u").setPassword("p").createIceServer(),
    )
}

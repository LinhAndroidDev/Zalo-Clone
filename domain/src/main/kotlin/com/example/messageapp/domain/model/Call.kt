package com.example.messageapp.domain.model

enum class CallType(val value: String) {
    VOICE("voice"),
    VIDEO("video");

    companion object {
        fun fromValue(value: String?): CallType = values().firstOrNull { it.value == value } ?: VOICE
    }
}

enum class CallStatus(val value: String) {
    RINGING("ringing"),
    ACCEPTED("accepted"),
    REJECTED("rejected"),
    CANCELLED("cancelled"),
    MISSED("missed"),
    BUSY("busy"),
    ENDED("ended");

    /** Cuộc gọi đã kết thúc (không còn đổ chuông/đang gọi). */
    val isFinished: Boolean get() = this != RINGING && this != ACCEPTED

    companion object {
        fun fromValue(value: String?): CallStatus = values().firstOrNull { it.value == value } ?: ENDED
    }
}

/** Mô tả phiên WebRTC (offer/answer). */
data class CallSessionDescription(
    val type: String = "",
    val sdp: String = "",
)

/** ICE candidate trao đổi giữa hai máy. */
data class CallIceCandidate(
    val sdpMid: String? = null,
    val sdpMLineIndex: Int = 0,
    val candidate: String = "",
)

data class Call(
    val callId: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val calleeName: String = "",
    val calleeAvatar: String = "",
    val type: CallType = CallType.VOICE,
    val status: CallStatus = CallStatus.RINGING,
    val offer: CallSessionDescription? = null,
    val answer: CallSessionDescription? = null,
    val createdAt: Long = 0L,
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
)

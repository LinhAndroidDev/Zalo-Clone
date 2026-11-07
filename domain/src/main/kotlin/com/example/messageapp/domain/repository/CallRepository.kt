package com.example.messageapp.domain.repository

import com.example.messageapp.domain.model.Call
import com.example.messageapp.domain.model.CallIceCandidate
import com.example.messageapp.domain.model.CallSessionDescription
import com.example.messageapp.domain.model.CallStatus
import kotlinx.coroutines.flow.Flow

/** Signaling cho cuộc gọi (Firestore) + báo cuộc gọi đến (FCM). */
interface CallRepository {
    /** Tạo cuộc gọi mới ở trạng thái RINGING kèm offer. */
    suspend fun createCall(call: Call): Result<Unit>

    /** Theo dõi realtime một cuộc gọi; phát null nếu document không tồn tại. */
    fun observeCall(callId: String): Flow<Call?>

    suspend fun getCall(callId: String): Call?

    /** Callee ghi answer và chuyển trạng thái sang ACCEPTED. */
    suspend fun answerCall(callId: String, answer: CallSessionDescription): Result<Unit>

    suspend fun updateStatus(callId: String, status: CallStatus): Result<Unit>

    suspend fun addIceCandidate(callId: String, fromCaller: Boolean, candidate: CallIceCandidate): Result<Unit>

    /** Theo dõi ICE candidate do bên kia gửi (fromCaller = true: candidate của caller). */
    fun observeIceCandidates(callId: String, fromCaller: Boolean): Flow<CallIceCandidate>

    /** User đang có cuộc gọi khác (đổ chuông hoặc đang gọi) hay không. */
    suspend fun isUserBusy(userId: String): Boolean

    /** Gửi push FCM báo cuộc gọi đến cho callee. */
    suspend fun notifyIncomingCall(call: Call): Result<Unit>
}

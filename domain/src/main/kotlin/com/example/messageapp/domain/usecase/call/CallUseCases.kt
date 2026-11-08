package com.example.messageapp.domain.usecase.call

import com.example.messageapp.domain.model.Call
import com.example.messageapp.domain.model.CallIceCandidate
import com.example.messageapp.domain.model.CallSessionDescription
import com.example.messageapp.domain.model.CallStatus
import com.example.messageapp.domain.model.CallType
import com.example.messageapp.domain.model.User
import com.example.messageapp.domain.repository.CallRepository
import com.example.messageapp.domain.repository.FriendRepository
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class CallException(val reason: Reason) : Exception(reason.name) {
    enum class Reason { NOT_FRIEND, CALLEE_BUSY, CALLER_BUSY, USER_NOT_FOUND, FAILED }
}

/**
 * Bắt đầu cuộc gọi: kiểm tra bạn bè + bận, tạo document RINGING kèm offer rồi gửi push.
 * Trả về [Call] đã tạo.
 */
class StartCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        calleeId: String,
        calleeName: String,
        calleeAvatar: String,
        type: CallType,
        offer: CallSessionDescription,
    ): Result<Call> {
        val myId = sessionRepository.getAuth()
        if (myId.isBlank() || calleeId.isBlank() || myId == calleeId) {
            return Result.failure(CallException(CallException.Reason.FAILED))
        }
        val friendship = suspendCancellableCoroutine<String> { cont ->
            friendRepository.getFriendshipStatus(myId, calleeId) { if (cont.isActive) cont.resume(it) }
        }
        if (friendship != "friend") return Result.failure(CallException(CallException.Reason.NOT_FRIEND))
        if (callRepository.isUserBusy(myId)) return Result.failure(CallException(CallException.Reason.CALLER_BUSY))
        if (callRepository.isUserBusy(calleeId)) return Result.failure(CallException(CallException.Reason.CALLEE_BUSY))

        val me: User = suspendCancellableCoroutine { cont ->
            userRepository.getUserById(myId, { if (cont.isActive) cont.resume(it) }, { if (cont.isActive) cont.resume(User()) })
        }
        if (me.keyAuth.isBlank()) return Result.failure(CallException(CallException.Reason.USER_NOT_FOUND))

        val call = Call(
            callId = UUID.randomUUID().toString(),
            callerId = myId,
            calleeId = calleeId,
            callerName = me.name,
            callerAvatar = me.avatar,
            calleeName = calleeName,
            calleeAvatar = calleeAvatar,
            type = type,
            status = CallStatus.RINGING,
            offer = offer,
            createdAt = System.currentTimeMillis(),
        )
        return callRepository.createCall(call).mapCatching {
            callRepository.notifyIncomingCall(call)
            call
        }
    }
}

class ObserveCallUseCase @Inject constructor(private val callRepository: CallRepository) {
    operator fun invoke(callId: String): Flow<Call?> = callRepository.observeCall(callId)
}

class GetCallUseCase @Inject constructor(private val callRepository: CallRepository) {
    suspend operator fun invoke(callId: String): Call? = callRepository.getCall(callId)
}

class AnswerCallUseCase @Inject constructor(private val callRepository: CallRepository) {
    suspend operator fun invoke(callId: String, answer: CallSessionDescription): Result<Unit> =
        callRepository.answerCall(callId, answer)
}

/** Từ chối / hủy / nhỡ / bận / kết thúc: chỉ khác nhau ở [CallStatus] truyền vào. */
class UpdateCallStatusUseCase @Inject constructor(private val callRepository: CallRepository) {
    suspend operator fun invoke(callId: String, status: CallStatus): Result<Unit> =
        callRepository.updateStatus(callId, status)
}

/** Ghi tin nhắn nhật ký cuộc gọi vào chat (chỉ người gọi ghi để không bị trùng). */
class SaveCallLogUseCase @Inject constructor(private val callRepository: CallRepository) {
    suspend operator fun invoke(call: Call, status: CallStatus, durationSec: Long): Result<Unit> =
        callRepository.saveCallLog(call, status, durationSec)
}

class AddIceCandidateUseCase @Inject constructor(private val callRepository: CallRepository) {
    suspend operator fun invoke(callId: String, fromCaller: Boolean, candidate: CallIceCandidate): Result<Unit> =
        callRepository.addIceCandidate(callId, fromCaller, candidate)
}

class ObserveIceCandidatesUseCase @Inject constructor(private val callRepository: CallRepository) {
    operator fun invoke(callId: String, fromCaller: Boolean): Flow<CallIceCandidate> =
        callRepository.observeIceCandidates(callId, fromCaller)
}

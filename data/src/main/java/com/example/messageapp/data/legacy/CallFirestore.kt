package com.example.messageapp.data.legacy

import com.example.messageapp.data.remote.ApiClient
import com.example.messageapp.data.remote.Token
import com.example.messageapp.data.remote.request.AndroidConfig
import com.example.messageapp.data.remote.request.Data
import com.example.messageapp.data.remote.request.MessageRequest
import com.example.messageapp.data.remote.request.NotificationData
import com.example.messageapp.domain.model.Call
import com.example.messageapp.domain.model.CallIceCandidate
import com.example.messageapp.domain.model.CallSessionDescription
import com.example.messageapp.domain.model.CallStatus
import com.example.messageapp.domain.model.CallType
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Callback
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Signaling cuộc gọi trên Firestore:
 * `calls/{callId}` + subcollection `callerCandidates` / `calleeCandidates`.
 */
@Singleton
class CallFirestore @Inject constructor() {
    private val db by lazy { Firebase.firestore }
    private val calls by lazy { db.collection(PATH_CALLS) }

    suspend fun createCall(call: Call) {
        calls.document(call.callId).set(call.toMap()).await()
    }

    fun observeCall(callId: String): Flow<Call?> = callbackFlow {
        val reg = calls.document(callId).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.takeIf { it.exists() }?.toCall())
        }
        awaitClose { reg.remove() }
    }

    suspend fun getCall(callId: String): Call? =
        calls.document(callId).get().await().takeIf { it.exists() }?.toCall()

    suspend fun answerCall(callId: String, answer: CallSessionDescription) {
        calls.document(callId).update(
            mapOf(
                FIELD_ANSWER to answer.toMap(),
                FIELD_STATUS to CallStatus.ACCEPTED.value,
                FIELD_STARTED_AT to System.currentTimeMillis(),
            )
        ).await()
    }

    suspend fun updateStatus(callId: String, status: CallStatus) {
        val updates = mutableMapOf<String, Any>(FIELD_STATUS to status.value)
        if (status.isFinished) updates[FIELD_ENDED_AT] = System.currentTimeMillis()
        calls.document(callId).update(updates).await()
    }

    suspend fun addIceCandidate(callId: String, fromCaller: Boolean, candidate: CallIceCandidate) {
        calls.document(callId).collection(candidatePath(fromCaller)).add(
            mapOf(
                "sdpMid" to candidate.sdpMid,
                "sdpMLineIndex" to candidate.sdpMLineIndex,
                "candidate" to candidate.candidate,
            )
        ).await()
    }

    fun observeIceCandidates(callId: String, fromCaller: Boolean): Flow<CallIceCandidate> = callbackFlow {
        val reg = calls.document(callId).collection(candidatePath(fromCaller))
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snap?.documentChanges
                    ?.filter { it.type == DocumentChange.Type.ADDED }
                    ?.forEach { change ->
                        val doc = change.document
                        trySend(
                            CallIceCandidate(
                                sdpMid = doc.getString("sdpMid"),
                                sdpMLineIndex = (doc.getLong("sdpMLineIndex") ?: 0L).toInt(),
                                candidate = doc.getString("candidate").orEmpty(),
                            )
                        )
                    }
            }
        awaitClose { reg.remove() }
    }

    /**
     * Bận nếu user có cuộc gọi RINGING/ACCEPTED còn "tươi". Lọc thời gian ở client để không cần
     * composite index; RINGING quá [RINGING_TIMEOUT_MS] coi như cuộc gọi cũ bị bỏ rơi.
     */
    suspend fun isUserBusy(userId: String): Boolean {
        val now = System.currentTimeMillis()
        val active = listOf(CallStatus.RINGING.value, CallStatus.ACCEPTED.value)
        for (field in listOf(FIELD_CALLER_ID, FIELD_CALLEE_ID)) {
            val docs = calls.whereEqualTo(field, userId).whereIn(FIELD_STATUS, active).get().await().documents
            val busy = docs.any { doc ->
                val status = doc.getString(FIELD_STATUS)
                val createdAt = doc.getLong(FIELD_CREATED_AT) ?: 0L
                status == CallStatus.ACCEPTED.value && doc.getLong(FIELD_ENDED_AT) == null &&
                    now - createdAt < ACCEPTED_MAX_AGE_MS ||
                    status == CallStatus.RINGING.value && now - createdAt < RINGING_TIMEOUT_MS
            }
            if (busy) return true
        }
        return false
    }

    /** Gửi push FCM (data-only) báo cuộc gọi đến, dùng lại cơ chế gửi push hiện có. */
    suspend fun notifyIncomingCall(call: Call) {
        val token = db.collection(PATH_TOKEN).document(call.calleeId).get().await()
            .toObject(Token::class.java)?.token?.trim().orEmpty()
        if (token.isEmpty()) error("Callee token not found")
        val body = if (call.type == CallType.VIDEO) "Cuộc gọi video đến" else "Cuộc gọi thoại đến"
        val request = MessageRequest(
            message = NotificationData(
                token = token,
                data = Data(
                    title = call.callerName,
                    body = body,
                    senderId = call.callerId,
                    recipientUserId = call.calleeId,
                    type = PUSH_TYPE_CALL,
                    callId = call.callId,
                    callType = call.type.value,
                    callerName = call.callerName,
                    callerAvatar = call.callerAvatar,
                ),
                android = AndroidConfig(priority = "high", ttl = "30s"),
            )
        )
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine<Unit> { cont ->
                val api = ApiClient.api
                if (api == null) {
                    cont.resumeWithException(IllegalStateException("Api not available"))
                    return@suspendCancellableCoroutine
                }
                val retrofitCall = api.sendMessage(request)
                cont.invokeOnCancellation { retrofitCall.cancel() }
                retrofitCall.enqueue(object : Callback<MessageRequest> {
                    override fun onFailure(call: retrofit2.Call<MessageRequest>, t: Throwable) {
                        if (cont.isActive) cont.resumeWithException(t)
                    }

                    override fun onResponse(
                        call: retrofit2.Call<MessageRequest>,
                        response: retrofit2.Response<MessageRequest>,
                    ) {
                        if (!cont.isActive) return
                        if (response.isSuccessful) cont.resume(Unit)
                        else cont.resumeWithException(IllegalStateException("FCM ${response.code()}"))
                    }
                })
            }
        }
    }

    private fun candidatePath(fromCaller: Boolean) = if (fromCaller) "callerCandidates" else "calleeCandidates"

    private fun Call.toMap(): Map<String, Any?> = mapOf(
        "callId" to callId,
        FIELD_CALLER_ID to callerId,
        FIELD_CALLEE_ID to calleeId,
        "callerName" to callerName,
        "callerAvatar" to callerAvatar,
        "calleeName" to calleeName,
        "calleeAvatar" to calleeAvatar,
        "type" to type.value,
        FIELD_STATUS to status.value,
        "offer" to offer?.toMap(),
        FIELD_ANSWER to answer?.toMap(),
        FIELD_CREATED_AT to createdAt,
    )

    private fun CallSessionDescription.toMap() = mapOf("type" to type, "sdp" to sdp)

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.sessionDescription(field: String): CallSessionDescription? {
        val map = get(field) as? Map<String, Any?> ?: return null
        return CallSessionDescription(type = map["type"] as? String ?: "", sdp = map["sdp"] as? String ?: "")
    }

    private fun DocumentSnapshot.toCall() = Call(
        callId = getString("callId") ?: id,
        callerId = getString(FIELD_CALLER_ID).orEmpty(),
        calleeId = getString(FIELD_CALLEE_ID).orEmpty(),
        callerName = getString("callerName").orEmpty(),
        callerAvatar = getString("callerAvatar").orEmpty(),
        calleeName = getString("calleeName").orEmpty(),
        calleeAvatar = getString("calleeAvatar").orEmpty(),
        type = CallType.fromValue(getString("type")),
        status = CallStatus.fromValue(getString(FIELD_STATUS)),
        offer = sessionDescription("offer"),
        answer = sessionDescription(FIELD_ANSWER),
        createdAt = getLong(FIELD_CREATED_AT) ?: 0L,
        startedAt = getLong(FIELD_STARTED_AT) ?: 0L,
        endedAt = getLong(FIELD_ENDED_AT) ?: 0L,
    )

    companion object {
        const val PATH_CALLS = "calls"
        const val PATH_TOKEN = "Tokens"
        const val PUSH_TYPE_CALL = "call"
        const val RINGING_TIMEOUT_MS = 60_000L
        const val ACCEPTED_MAX_AGE_MS = 6 * 60 * 60 * 1000L
        private const val FIELD_CALLER_ID = "callerId"
        private const val FIELD_CALLEE_ID = "calleeId"
        private const val FIELD_STATUS = "status"
        private const val FIELD_ANSWER = "answer"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_STARTED_AT = "startedAt"
        private const val FIELD_ENDED_AT = "endedAt"
    }
}

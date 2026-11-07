package com.example.messageapp.data.repository

import com.example.messageapp.data.legacy.CallFirestore
import com.example.messageapp.domain.model.Call
import com.example.messageapp.domain.model.CallIceCandidate
import com.example.messageapp.domain.model.CallSessionDescription
import com.example.messageapp.domain.model.CallStatus
import com.example.messageapp.domain.repository.CallRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepositoryImpl @Inject constructor(
    private val firestore: CallFirestore,
) : CallRepository {

    override suspend fun createCall(call: Call) = runCatchingCancellable { firestore.createCall(call) }

    override fun observeCall(callId: String): Flow<Call?> = firestore.observeCall(callId)

    override suspend fun getCall(callId: String): Call? =
        runCatchingCancellable { firestore.getCall(callId) }.getOrNull()

    override suspend fun answerCall(callId: String, answer: CallSessionDescription) =
        runCatchingCancellable { firestore.answerCall(callId, answer) }

    override suspend fun updateStatus(callId: String, status: CallStatus) =
        runCatchingCancellable { firestore.updateStatus(callId, status) }

    override suspend fun addIceCandidate(callId: String, fromCaller: Boolean, candidate: CallIceCandidate) =
        runCatchingCancellable { firestore.addIceCandidate(callId, fromCaller, candidate) }

    override fun observeIceCandidates(callId: String, fromCaller: Boolean): Flow<CallIceCandidate> =
        firestore.observeIceCandidates(callId, fromCaller)

    override suspend fun isUserBusy(userId: String): Boolean =
        runCatchingCancellable { firestore.isUserBusy(userId) }.getOrDefault(false)

    override suspend fun notifyIncomingCall(call: Call) =
        runCatchingCancellable { firestore.notifyIncomingCall(call) }

    /** Như runCatching nhưng không nuốt CancellationException của coroutine. */
    private inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}

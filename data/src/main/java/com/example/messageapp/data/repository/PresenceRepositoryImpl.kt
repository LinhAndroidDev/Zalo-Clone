package com.example.messageapp.data.repository

import com.example.messageapp.data.legacy.PresenceManager
import com.example.messageapp.data.mapper.EntityMapper
import com.example.messageapp.domain.repository.PresenceRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceRepositoryImpl @Inject constructor(
    private val presenceManager: PresenceManager,
) : PresenceRepository {

    override fun connect(userId: String) = presenceManager.connect(userId)

    override fun disconnect(userId: String) = presenceManager.disconnect(userId)

    override fun observePresence(userId: String): Flow<com.example.messageapp.domain.model.UserPresence> =
        callbackFlow {
            val unsubscribe = presenceManager.observePresence(userId) { presence ->
                trySend(EntityMapper.toDomain(presence))
            }
            awaitClose { unsubscribe() }
        }
}

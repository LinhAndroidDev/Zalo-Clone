package com.example.messageapp.data.repository

import com.example.messageapp.data.legacy.FireBaseInstance
import com.example.messageapp.data.mapper.EntityMapper
import com.example.messageapp.domain.model.Conversation
import com.example.messageapp.domain.repository.ConversationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor() : ConversationRepository {

    override fun observeInbox(userId: String): Flow<List<Conversation>> = callbackFlow {
        val registration = FireBaseInstance.getListConversation(
            userId = userId,
            success = { snapshot ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(com.example.messageapp.data.firestore.Conversation::class.java)
                        ?.let { EntityMapper.toDomain(it) }
                }.orEmpty()
                trySend(list)
            },
            failure = { close(RuntimeException(it)) },
        )
        awaitClose { registration.remove() }
    }

    override fun observeConversation(friendId: String, userId: String): Flow<Conversation> = callbackFlow {
        val registration = FireBaseInstance.getConversationRlt(friendId, userId) { conversation ->
            trySend(EntityMapper.toDomain(conversation))
        }
        awaitClose { registration.remove() }
    }

    override fun getConversation(friendId: String, userId: String, onSuccess: (Conversation) -> Unit) {
        FireBaseInstance.getConversation(friendId, userId) { onSuccess(EntityMapper.toDomain(it)) }
    }

    override fun getConversationRealtime(friendId: String, userId: String, onSuccess: (Conversation) -> Unit) {
        FireBaseInstance.getConversationRlt(friendId, userId) { onSuccess(EntityMapper.toDomain(it)) }
    }

    override fun getUnreadCount(userId: String, onResult: (Int) -> Unit) {
        FireBaseInstance.getNumberUnreadMessages(userId, onResult)
    }
}

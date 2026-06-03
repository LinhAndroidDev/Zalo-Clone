package com.example.messageapp.domain.usecase.inbox

import com.example.messageapp.domain.model.Conversation
import com.example.messageapp.domain.repository.ConversationRepository
import com.example.messageapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveInboxUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke() = conversationRepository.observeInbox(sessionRepository.getAuth())
}

class GetUnreadCountUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(onResult: (Int) -> Unit) =
        conversationRepository.getUnreadCount(sessionRepository.getAuth(), onResult)
}

class GetConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    fun once(friendId: String, userId: String, onSuccess: (Conversation) -> Unit) {
        conversationRepository.getConversation(friendId, userId, onSuccess)
    }

    fun realtime(friendId: String, userId: String, onSuccess: (Conversation) -> Unit) {
        conversationRepository.getConversationRealtime(friendId, userId, onSuccess)
    }

    fun observe(friendId: String, userId: String): Flow<Conversation> =
        conversationRepository.observeConversation(friendId, userId)
}

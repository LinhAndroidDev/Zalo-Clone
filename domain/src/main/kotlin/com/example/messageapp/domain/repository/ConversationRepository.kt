package com.example.messageapp.domain.repository

import com.example.messageapp.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeInbox(userId: String): Flow<List<Conversation>>
    fun observeConversation(friendId: String, userId: String): Flow<Conversation>
    fun getConversation(friendId: String, userId: String, onSuccess: (Conversation) -> Unit)
    fun getConversationRealtime(friendId: String, userId: String, onSuccess: (Conversation) -> Unit)
    fun getUnreadCount(userId: String, onResult: (Int) -> Unit)
}

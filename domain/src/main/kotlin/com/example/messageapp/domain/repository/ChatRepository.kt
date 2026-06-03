package com.example.messageapp.domain.repository

import com.example.messageapp.domain.model.Conversation
import com.example.messageapp.domain.model.EmotionType
import com.example.messageapp.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun messageThreadDocumentId(conversation: Conversation, userId: String): String
    fun observeMessages(
        conversation: Conversation,
        userId: String,
    ): Flow<List<Message>>
    fun sendMessage(
        message: Message,
        userId: String,
        time: String,
        conversation: Conversation,
        nameSender: String,
        sendFirst: Boolean,
    )
    fun removeMessage(conversation: Conversation, userId: String, time: String)
    fun toggleMessageReaction(
        time: String,
        conversation: Conversation,
        userId: String,
        type: EmotionType,
        onFailure: (String) -> Unit,
    )
    fun updateTyping(conversation: Conversation, userId: String, typing: Boolean)
    fun observeTyping(conversation: Conversation, userId: String): Flow<Boolean>
    fun markSeen(message: Message, conversation: Conversation, userId: String)
}

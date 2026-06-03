package com.example.messageapp.data.repository

import com.example.messageapp.data.firestore.TypeMessage
import com.example.messageapp.data.legacy.FireBaseInstance
import com.example.messageapp.data.mapper.EntityMapper
import com.example.messageapp.domain.model.Conversation
import com.example.messageapp.domain.model.EmotionType
import com.example.messageapp.domain.model.Message
import com.example.messageapp.domain.repository.ChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor() : ChatRepository {

    override fun messageThreadDocumentId(conversation: Conversation, userId: String): String {
        val fs = EntityMapper.toFirestore(conversation)
        return FireBaseInstance.messageThreadDocumentId(fs, userId)
    }

    override fun observeMessages(conversation: Conversation, userId: String): Flow<List<Message>> =
        callbackFlow {
            val fsConversation = EntityMapper.toFirestore(conversation)
            val roomId = FireBaseInstance.messageThreadDocumentId(fsConversation, userId)
            val registration = FireBaseInstance.getMessage(
                idRoom = roomId,
                success = { snapshot ->
                    val messages = snapshot?.documents?.mapNotNull { document ->
                        val raw = document.toObject(com.example.messageapp.data.firestore.Message::class.java)
                            ?: return@mapNotNull null
                        if (!isMessageInConversation(raw, fsConversation, userId)) return@mapNotNull null
                        val timeResolved = raw.time.ifBlank { document.id }
                        EntityMapper.toDomain(raw.copy(time = timeResolved))
                    }.orEmpty()
                    trySend(messages)
                },
                failure = { close(RuntimeException(it)) },
            )
            awaitClose { registration.remove() }
        }

    override fun sendMessage(
        message: Message,
        userId: String,
        time: String,
        conversation: Conversation,
        nameSender: String,
        sendFirst: Boolean,
    ) {
        val fsMessage = EntityMapper.toFirestore(message)
        val fsConversation = EntityMapper.toFirestore(conversation)
        FireBaseInstance.sendMessage(
            message = fsMessage,
            userId = userId,
            time = time,
            conversation = fsConversation,
            nameSender = nameSender,
            type = TypeMessage.entries.getOrElse(message.type) { TypeMessage.MESSAGE },
            sendFirst = sendFirst,
            success = {},
        )
    }

    override fun removeMessage(conversation: Conversation, userId: String, time: String) {
        FireBaseInstance.removeMessage(
            conversation = EntityMapper.toFirestore(conversation),
            userId = userId,
            time = time,
        )
    }

    override fun toggleMessageReaction(
        time: String,
        conversation: Conversation,
        userId: String,
        type: EmotionType,
        onFailure: (String) -> Unit,
    ) {
        val idRoom = messageThreadDocumentId(conversation, userId)
        FireBaseInstance.toggleMessageReaction(
            time = time,
            idRoom = idRoom,
            userId = userId,
            type = EntityMapper.toFirestore(type),
            onFailure = onFailure,
        )
    }

    override fun updateTyping(conversation: Conversation, userId: String, typing: Boolean) {
        if (conversation.isGroupThread()) {
            FireBaseInstance.updateGroupTyping(conversation.friendId, userId, typing)
        } else {
            FireBaseInstance.updateTypingMessage(userId, conversation.friendId, typing)
        }
    }

    override fun observeTyping(conversation: Conversation, userId: String): Flow<Boolean> =
        callbackFlow {
            if (conversation.isGroupThread()) {
                val registration = FireBaseInstance.observeGroupTyping(conversation.friendId, userId) { show ->
                    trySend(show)
                }
                awaitClose { registration.remove() }
            } else {
                val registration = FireBaseInstance.getConversationRlt(
                    friendId = conversation.friendId,
                    userId = userId,
                    success = { cvt -> trySend(cvt.typing) },
                )
                awaitClose { registration.remove() }
            }
        }

    override fun markSeen(message: Message, conversation: Conversation, userId: String) {
        if (conversation.isGroupThread()) {
            if (message.time.isNotBlank()) {
                FireBaseInstance.markGroupMessageRead(userId, conversation.friendId, message.time)
            }
            return
        }
        if (message.sender != userId) {
            FireBaseInstance.getConversation(
                friendId = userId,
                userId = conversation.friendId,
                success = { cvt ->
                    if (!cvt.isSeenMessage() && cvt.sender == conversation.friendId) {
                        FireBaseInstance.seenMessage(userId, conversation.friendId)
                    }
                },
            )
        }
    }

    private fun isMessageInConversation(
        message: com.example.messageapp.data.firestore.Message,
        conversation: com.example.messageapp.data.firestore.Conversation,
        userId: String,
    ): Boolean {
        if (conversation.isGroupThread()) {
            return message.receiver == conversation.friendId
        }
        return message.sender == userId && message.receiver == conversation.friendId ||
            message.receiver == userId && message.sender == conversation.friendId
    }
}

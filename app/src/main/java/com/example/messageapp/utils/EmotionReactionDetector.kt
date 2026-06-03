package com.example.messageapp.utils

import com.example.messageapp.domain.chat.EmotionReactionDetector as DomainEmotionReactionDetector
import com.example.messageapp.mapper.ChatUiMapper
import com.example.messageapp.model.EmotionType
import com.example.messageapp.model.Message

object EmotionReactionDetector {

    data class RemoteReactionChange(
        val messageTime: String,
        val reactorUserId: String,
        val type: EmotionType,
    )

    fun detectRemoteReactionChanges(
        previous: List<Message>,
        current: List<Message>,
        myUserId: String,
    ): List<RemoteReactionChange> =
        DomainEmotionReactionDetector.detectRemoteReactionChanges(
            previous = previous.map { ChatUiMapper.toDomain(it) },
            current = current.map { ChatUiMapper.toDomain(it) },
            myUserId = myUserId,
        ).map {
            RemoteReactionChange(
                messageTime = it.messageTime,
                reactorUserId = it.reactorUserId,
                type = ChatUiMapper.toUi(it.type),
            )
        }
}

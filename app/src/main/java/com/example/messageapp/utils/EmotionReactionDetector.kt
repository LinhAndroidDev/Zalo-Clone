package com.example.messageapp.utils

import com.example.messageapp.model.Emotion
import com.example.messageapp.model.EmotionType
import com.example.messageapp.model.Message

object EmotionReactionDetector {

    data class RemoteReactionChange(
        val messageTime: String,
        val reactorUserId: String,
        val type: EmotionType,
    )

    /**
     * Phát hiện cảm xúc vừa được gắn/đổi bởi người khác (không phải [myUserId]).
     * Bỏ qua lần collect đầu ([previous] rỗng) để tránh nổ hàng loạt khi mở chat.
     */
    fun detectRemoteReactionChanges(
        previous: List<Message>,
        current: List<Message>,
        myUserId: String,
    ): List<RemoteReactionChange> {
        if (previous.isEmpty() || myUserId.isBlank()) return emptyList()

        val previousByTime = previous.associateBy { it.time }
        val changes = mutableListOf<RemoteReactionChange>()

        for (currentMsg in current) {
            val previousMsg = previousByTime[currentMsg.time] ?: continue
            if (previousMsg.emotion == currentMsg.emotion) continue

            val userIds = reactorUserIds(previousMsg.emotion) + reactorUserIds(currentMsg.emotion)
            for (userId in userIds) {
                if (userId == myUserId) continue
                val type = reactionChangeForUser(
                    previousMsg.emotion,
                    currentMsg.emotion,
                    userId,
                ) ?: continue
                changes.add(
                    RemoteReactionChange(
                        messageTime = currentMsg.time,
                        reactorUserId = userId,
                        type = type,
                    ),
                )
            }
        }
        return changes
    }

    private fun reactorUserIds(emotion: Emotion?): Set<String> {
        val e = emotion ?: return emptySet()
        return buildSet {
            addAll(e.favourite.keys)
            addAll(e.like.keys)
            addAll(e.laugh.keys)
            addAll(e.cry.keys)
            addAll(e.angry.keys)
        }
    }

    /** Trả về loại cảm xúc mới nếu [userId] vừa gắn hoặc đổi; null nếu gỡ hoặc không đổi. */
    private fun reactionChangeForUser(
        previous: Emotion?,
        current: Emotion?,
        userId: String,
    ): EmotionType? {
        val oldType = (previous ?: Emotion()).findUserReaction(userId)
        val newType = (current ?: Emotion()).findUserReaction(userId) ?: return null
        if (newType == oldType) return null
        return newType
    }
}

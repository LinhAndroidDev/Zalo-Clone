package com.example.messageapp.domain.usecase.chat

import com.example.messageapp.domain.model.Conversation
import com.example.messageapp.domain.model.Emotion
import com.example.messageapp.domain.model.EmotionType
import com.example.messageapp.domain.model.Message
import com.example.messageapp.domain.model.PinnedMessage
import com.example.messageapp.domain.repository.ChatRepository
import com.example.messageapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(conversation: Conversation): Flow<List<Message>> =
        chatRepository.observeMessages(conversation, sessionRepository.getAuth())
}

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(message: Message, time: String, conversation: Conversation, sendFirst: Boolean) {
        chatRepository.sendMessage(
            message = message,
            userId = sessionRepository.getAuth(),
            time = time,
            conversation = conversation,
            nameSender = sessionRepository.getNameUser(),
            sendFirst = sendFirst,
        )
    }
}

class ForwardMessageUseCase @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        source: Message,
        targets: List<Conversation>,
        timeProvider: () -> String,
    ) {
        if (targets.isEmpty() || source.type == SYSTEM_MESSAGE_TYPE) return
        val userId = sessionRepository.getAuth()
        targets.forEach { conversation ->
            val time = timeProvider()
            val forwarded = buildForwardedMessage(source, conversation, userId, time)
            val sendFirst = conversation.message.isBlank()
            sendMessageUseCase(forwarded, time, conversation, sendFirst)
        }
    }

    private fun buildForwardedMessage(
        source: Message,
        conversation: Conversation,
        userId: String,
        time: String,
    ): Message {
        val forwardFromId = source.sender
        val forwardFromName = when {
            forwardFromId == userId -> sessionRepository.getNameUser()
            else -> ""
        }
        return Message(
            message = source.message,
            receiver = conversation.friendId,
            sender = userId,
            time = time,
            mentions = source.mentions,
            photos = source.photos,
            photoSizes = source.photoSizes,
            singlePhoto = source.singlePhoto,
            audio = source.audio,
            type = source.type,
            replyTo = source.replyTo,
            forwardFromId = forwardFromId,
            forwardFromName = forwardFromName,
        )
    }

    companion object {
        private const val SYSTEM_MESSAGE_TYPE = 4
    }
}

class ToggleMessageReactionUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
) {
    data class Result(
        val optimisticMessages: List<Message>,
        val reactionApplied: Boolean,
        val type: EmotionType,
    )

    operator fun invoke(
        messages: List<Message>,
        time: String,
        conversation: Conversation,
        type: EmotionType,
        onFailure: (String) -> Unit,
    ): Result? {
        if (time.isBlank()) return null
        val userId = sessionRepository.getAuth()
        val index = messages.indexOfFirst { it.time == time }
        if (index < 0) return null
        val target = messages[index]
        val merged = (target.emotion ?: Emotion()).toggleUserReaction(userId, type)
        val reactionApplied = merged.findUserReaction(userId) == type
        val updated = target.copy(emotion = merged.takeUnless { it.emotionEmpty() })
        val optimistic = messages.toMutableList().also { it[index] = updated }
        chatRepository.toggleMessageReaction(time, conversation, userId, type, onFailure)
        return Result(optimistic, reactionApplied, type)
    }
}

class ObserveTypingUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(conversation: Conversation): Flow<Boolean> =
        chatRepository.observeTyping(conversation, sessionRepository.getAuth())
}

class UpdateTypingUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(conversation: Conversation, typing: Boolean) {
        chatRepository.updateTyping(conversation, sessionRepository.getAuth(), typing)
    }
}

class MarkMessageReadUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(message: Message, conversation: Conversation) {
        chatRepository.markSeen(message, conversation, sessionRepository.getAuth())
    }
}

class RemoveMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(conversation: Conversation, time: String) {
        chatRepository.removeMessage(conversation, sessionRepository.getAuth(), time)
    }
}

class ObservePinnedMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(conversation: Conversation): Flow<List<PinnedMessage>> =
        chatRepository.observePinnedMessages(conversation, sessionRepository.getAuth())
}

@Deprecated(
    message = "Renamed to ObservePinnedMessagesUseCase",
    replaceWith = ReplaceWith("ObservePinnedMessagesUseCase"),
)
class ObservePinnedMessageUseCase @Inject constructor(
    private val delegate: ObservePinnedMessagesUseCase,
) {
    operator fun invoke(conversation: Conversation): Flow<List<PinnedMessage>> =
        delegate(conversation)
}

class PinMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(message: Message, conversation: Conversation) {
        if (message.time.isBlank() || message.type == SYSTEM_MESSAGE_TYPE) return
        chatRepository.pinMessage(
            message = message,
            conversation = conversation,
            userId = sessionRepository.getAuth(),
            userName = sessionRepository.getNameUser(),
        )
    }

    companion object {
        private const val SYSTEM_MESSAGE_TYPE = 4
        const val MAX_PINNED_MESSAGES = 10
    }
}

class UnpinMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(conversation: Conversation, messageTime: String) {
        if (messageTime.isBlank()) return
        chatRepository.unpinMessage(conversation, sessionRepository.getAuth(), messageTime)
    }
}

class ReorderPinnedMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(conversation: Conversation, orderedTimes: List<String>) {
        if (orderedTimes.isEmpty()) return
        chatRepository.reorderPinnedMessages(
            conversation = conversation,
            userId = sessionRepository.getAuth(),
            orderedTimes = orderedTimes,
        )
    }
}

class ObservePresenceUseCase @Inject constructor(
    private val presenceRepository: com.example.messageapp.domain.repository.PresenceRepository,
) {
    operator fun invoke(userId: String) = presenceRepository.observePresence(userId)
}

class CreateGroupUseCase @Inject constructor(
    private val groupChatRepository: com.example.messageapp.domain.repository.GroupChatRepository,
) {
    operator fun invoke(
        name: String,
        creatorId: String,
        creatorAvatar: String,
        welcomeMessage: String,
        welcomeInboxPerson: String,
        memberIds: List<String>,
        onSuccess: (String, Conversation) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        groupChatRepository.createGroup(
            name, creatorId, creatorAvatar, welcomeMessage, welcomeInboxPerson, memberIds,
            onSuccess, onFailure,
        )
    }
}

class ObserveGroupReadStatusUseCase @Inject constructor(
    private val groupChatRepository: com.example.messageapp.domain.repository.GroupChatRepository,
) {
    operator fun invoke(groupId: String) = groupChatRepository.observeGroupMemberRead(groupId)
}

class LoadGroupMembersUseCase @Inject constructor(
    private val groupChatRepository: com.example.messageapp.domain.repository.GroupChatRepository,
) {
    operator fun invoke(groupId: String, onSuccess: (List<com.example.messageapp.domain.model.User>) -> Unit, onFailure: (String) -> Unit = {}) {
        groupChatRepository.loadGroupMembers(groupId, onSuccess, onFailure)
    }
}

class AddGroupMembersUseCase @Inject constructor(
    private val groupChatRepository: com.example.messageapp.domain.repository.GroupChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        groupId: String,
        newMemberIds: List<String>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit = {},
    ) {
        groupChatRepository.addGroupMembers(
            groupId = groupId,
            newMemberIds = newMemberIds,
            inviterId = sessionRepository.getAuth(),
            onSuccess = onSuccess,
            onFailure = onFailure,
        )
    }
}

class RemoveGroupMemberUseCase @Inject constructor(
    private val groupChatRepository: com.example.messageapp.domain.repository.GroupChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        groupId: String,
        memberId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit = {},
    ) {
        groupChatRepository.removeGroupMember(
            groupId = groupId,
            memberId = memberId,
            actorId = sessionRepository.getAuth(),
            onSuccess = onSuccess,
            onFailure = onFailure,
        )
    }
}

class LeaveGroupUseCase @Inject constructor(
    private val groupChatRepository: com.example.messageapp.domain.repository.GroupChatRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        groupId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit = {},
    ) {
        groupChatRepository.leaveGroup(
            groupId = groupId,
            userId = sessionRepository.getAuth(),
            onSuccess = onSuccess,
            onFailure = onFailure,
        )
    }
}

class UploadChatMediaUseCase @Inject constructor(
    private val mediaUploadRepository: com.example.messageapp.domain.repository.MediaUploadRepository,
) {
    fun uploadPhotos(
        uriStrings: List<String>,
        roomId: List<String>,
        onProgress: (Int) -> Unit,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) = mediaUploadRepository.uploadListPhoto(uriStrings, roomId, onProgress, onSuccess, onFailure)

    fun uploadAudio(
        uriString: String,
        roomId: List<String>,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) = mediaUploadRepository.uploadAudio(uriString, roomId, onProgress, onSuccess, onFailure)
}

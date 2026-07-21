package com.example.messageapp.domain

import com.example.messageapp.domain.model.Emotion
import com.example.messageapp.domain.model.EmotionType
import com.example.messageapp.domain.model.Message
import com.example.messageapp.domain.chat.EmotionReactionDetector
import com.example.messageapp.domain.chat.MentionParser
import com.example.messageapp.domain.model.Conversation
import com.example.messageapp.domain.model.PinnedMessage
import com.example.messageapp.domain.repository.ChatRepository
import com.example.messageapp.domain.usecase.chat.ToggleMessageReactionUseCase
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmotionReactionDetectorTest {

    @Test
    fun detectRemoteReactionChanges_skipsFirstSnapshot() {
        val current = listOf(msg("1", emotion = emotionWith(user = "other", type = EmotionType.LIKE)))
        val changes = EmotionReactionDetector.detectRemoteReactionChanges(
            previous = emptyList(),
            current = current,
            myUserId = "me",
        )
        assertTrue(changes.isEmpty())
    }

    @Test
    fun detectRemoteReactionChanges_detectsOtherUserReaction() {
        val previous = listOf(msg("1"))
        val current = listOf(msg("1", emotion = emotionWith(user = "other", type = EmotionType.LIKE)))
        val changes = EmotionReactionDetector.detectRemoteReactionChanges(
            previous = previous,
            current = current,
            myUserId = "me",
        )
        assertEquals(1, changes.size)
        assertEquals("other", changes.first().reactorUserId)
        assertEquals(EmotionType.LIKE, changes.first().type)
    }

    private fun msg(time: String, emotion: Emotion? = null) = Message(
        time = time,
        sender = "other",
        receiver = "room",
        emotion = emotion,
    )

    private fun emotionWith(user: String, type: EmotionType): Emotion =
        Emotion().applyUserReaction(user, type)
}

class MentionParserTest {

    @Test
    fun detectActiveMentionQuery_findsAtSymbol() {
        val text = "hello @lin"
        val query = MentionParser.detectActiveMentionQuery(text, cursor = text.length)!!
        assertEquals(6, query.startIndex)
        assertEquals("lin", query.query)
    }

    @Test
    fun hasAllMention_detectsAllToken() {
        val mentions = listOf(
            com.example.messageapp.domain.model.MessageMention(userId = MentionParser.ALL_USER_ID),
        )
        assertTrue(MentionParser.hasAllMention(mentions))
    }
}

class ToggleMessageReactionUseCaseTest {

    @Test
    fun invoke_appliesOptimisticReaction() {
        val repo = FakeChatRepository()
        val session = FakeSessionRepository(auth = "me")
        val useCase = ToggleMessageReactionUseCase(repo, session)
        val messages = listOf(Message(time = "t1", sender = "other", receiver = "room"))
        val result = useCase(
            messages = messages,
            time = "t1",
            conversation = Conversation(friendId = "friend"),
            type = EmotionType.LIKE,
            onFailure = {},
        )
        assertTrue(result!!.reactionApplied)
        assertEquals(1, result.optimisticMessages.first().emotion?.like?.size)
    }
}

private class FakeSessionRepository(
    private val auth: String,
) : com.example.messageapp.domain.repository.SessionRepository {
    override fun saveLanguageSelected(language: com.example.messageapp.domain.model.AppLanguage) {}
    override fun getLanguageSelected() = com.example.messageapp.domain.model.AppLanguage.VIETNAMESE
    override fun saveAuth(auth: String) {}
    override fun getAuth() = auth
    override fun saveNameUser(name: String) {}
    override fun getNameUser() = "Me"
    override fun saveChannelId(channelId: Int) {}
    override fun getChannelId() = 0
    override fun saveStatusLoggedIn(status: Boolean) {}
    override fun getStatusLoggedIn() = true
    override fun saveLastSeenFriendRequestAt(time: Long) {}
    override fun getLastSeenFriendRequestAt() = 0L
    override fun saveLastSeenDiaryNotificationAt(time: Long) {}
    override fun getLastSeenDiaryNotificationAt() = 0L
}

private class FakeChatRepository : ChatRepository {
    override fun messageThreadDocumentId(conversation: Conversation, userId: String) = "room"
    override fun observeMessages(conversation: Conversation, userId: String) =
        flowOf(emptyList<Message>())
    override fun observePinnedMessages(conversation: Conversation, userId: String) =
        flowOf(emptyList<PinnedMessage>())
    override fun sendMessage(
        message: Message,
        userId: String,
        time: String,
        conversation: Conversation,
        nameSender: String,
        sendFirst: Boolean,
    ) {}
    override fun removeMessage(conversation: Conversation, userId: String, time: String) {}
    override fun pinMessage(
        message: Message,
        conversation: Conversation,
        userId: String,
        userName: String,
    ) {}
    override fun unpinMessage(conversation: Conversation, userId: String, messageTime: String) {}
    override fun reorderPinnedMessages(
        conversation: Conversation,
        userId: String,
        orderedTimes: List<String>,
    ) {}
    override fun toggleMessageReaction(
        time: String,
        conversation: Conversation,
        userId: String,
        type: EmotionType,
        onFailure: (String) -> Unit,
    ) {}
    override fun updateTyping(conversation: Conversation, userId: String, typing: Boolean) {}
    override fun observeTypingUsers(conversation: Conversation, userId: String) =
        flowOf(emptyList<String>())
    override fun observeTyping(conversation: Conversation, userId: String) =
        flowOf(false)
    override fun markSeen(message: Message, conversation: Conversation, userId: String) {}
}

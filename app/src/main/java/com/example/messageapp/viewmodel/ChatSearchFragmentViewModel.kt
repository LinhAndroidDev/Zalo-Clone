package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.data.legacy.DateUtils
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.repository.UserRepository
import com.example.messageapp.domain.usecase.chat.LoadGroupMembersUseCase
import com.example.messageapp.domain.usecase.chat.ObserveMessagesUseCase
import com.example.messageapp.mapper.ChatUiMapper
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Message
import com.example.messageapp.model.User
import com.example.messageapp.utils.ChatMessageSearchHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatSearchResultItem(
    val messageTime: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val messageText: String,
    val displayTime: String,
    val matchStart: Int,
    val matchEnd: Int,
)

data class ChatSearchUiState(
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val query: String = "",
    val results: List<ChatSearchResultItem> = emptyList(),
)

@HiltViewModel
class ChatSearchFragmentViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val loadGroupMembersUseCase: LoadGroupMembersUseCase,
    private val userRepository: UserRepository,
) : BaseViewModel() {

    private var messagesJob: kotlinx.coroutines.Job? = null
    private var cachedMessages: List<Message> = emptyList()
    private var conversation: Conversation? = null
    private val memberCache = mutableMapOf<String, User>()

    private val _uiState = MutableStateFlow(ChatSearchUiState())
    val uiState = _uiState.asStateFlow()

    fun init(conversation: Conversation) {
        if (this.conversation?.friendId == conversation.friendId) return
        this.conversation = conversation
        memberCache.clear()
        cachedMessages = emptyList()
        _uiState.value = ChatSearchUiState()

        if (conversation.isGroupThread()) {
            loadGroupMembersUseCase(
                groupId = conversation.friendId,
                onSuccess = { members ->
                    members.forEach { user ->
                        memberCache[user.keyAuth.orEmpty()] = ChatUiMapper.toUi(user)
                    }
                },
            )
        } else {
            val myId = sessionRepository.getAuth()
            if (myId.isNotBlank()) {
                userRepository.getInfoUser(
                    myId,
                    onSuccess = { user -> memberCache[myId] = ChatUiMapper.toUi(user) },
                )
            }
        }

        messagesJob?.cancel()
        val domainConversation = ChatUiMapper.toDomain(conversation)
        messagesJob = viewModelScope.launch {
            observeMessagesUseCase(domainConversation)
                .catch { showError(it.message.orEmpty()) }
                .collect { domainMessages ->
                    cachedMessages = ChatUiMapper.toUiList(domainMessages)
                }
        }
    }

    fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = ChatSearchUiState()
            return
        }

        val conversation = conversation ?: return
        val matches = ChatMessageSearchHelper.findMatchMessages(cachedMessages, trimmed)
        val results = matches.map { match ->
            val senderId = match.message.sender
            ChatSearchResultItem(
                messageTime = match.message.time,
                senderId = senderId,
                senderName = resolveSenderName(conversation, senderId),
                senderAvatar = resolveSenderAvatar(conversation, senderId),
                messageText = match.message.message,
                displayTime = DateUtils.convertTimeToHour(match.message.time),
                matchStart = match.matchStart,
                matchEnd = match.matchEnd,
            )
        }
        _uiState.value = ChatSearchUiState(
            hasSearched = true,
            query = trimmed,
            results = results,
        )
    }

    fun loadMissingSender(userId: String, onLoaded: () -> Unit) {
        if (userId.isBlank() || memberCache.containsKey(userId)) {
            onLoaded()
            return
        }
        userRepository.getInfoUser(
            userId,
            onSuccess = { user ->
                memberCache[userId] = ChatUiMapper.toUi(user)
                onLoaded()
            },
            onFailure = { onLoaded() },
        )
    }

    fun senderInfo(userId: String): Pair<String, String> {
        val cached = memberCache[userId]
        return (cached?.name.orEmpty().ifBlank { userId.takeLast(8) }) to cached?.avatar.orEmpty()
    }

    private fun resolveSenderName(conversation: Conversation, senderId: String): String {
        val myId = sessionRepository.getAuth()
        if (senderId == myId) {
            return sessionRepository.getNameUser().ifBlank { "Bạn" }
        }
        if (!conversation.isGroupThread()) {
            return conversation.name
        }
        return memberCache[senderId]?.name.orEmpty().ifBlank { senderId.takeLast(8) }
    }

    private fun resolveSenderAvatar(conversation: Conversation, senderId: String): String {
        if (!conversation.isGroupThread()) {
            val myId = sessionRepository.getAuth()
            return if (senderId == myId) {
                memberCache[myId]?.avatar.orEmpty()
            } else {
                conversation.friendImage
            }
        }
        return memberCache[senderId]?.avatar.orEmpty()
    }

    override fun onCleared() {
        messagesJob?.cancel()
        super.onCleared()
    }
}

package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.UserRepository
import com.example.messageapp.domain.usecase.chat.ForwardMessageUseCase
import com.example.messageapp.domain.usecase.inbox.ObserveInboxUseCase
import com.example.messageapp.mapper.ChatUiMapper
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Message
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.GroupAvatarLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForwardMessageViewModel @Inject constructor(
    private val observeInboxUseCase: ObserveInboxUseCase,
    private val forwardMessageUseCase: ForwardMessageUseCase,
    private val userRepository: UserRepository,
    val groupAvatarLoader: GroupAvatarLoader,
) : BaseViewModel() {

    private var inboxJob: Job? = null
    private val avatarRequestedIds = mutableSetOf<String>()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations = _conversations.asStateFlow()

    private val _avatarMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val avatarMap = _avatarMap.asStateFlow()

    private val _forwardComplete = MutableStateFlow(false)
    val forwardComplete = _forwardComplete.asStateFlow()

    fun loadConversations(excludeConversationId: String) {
        inboxJob?.cancel()
        inboxJob = viewModelScope.launch {
            observeInboxUseCase()
                .catch { showError(it.message.orEmpty()) }
                .collect { list ->
                    val uiList = list
                        .map { ChatUiMapper.toUi(it) }
                        .filter { it.friendId.isNotBlank() && it.friendId != excludeConversationId }
                    _conversations.value = uiList
                    syncAvatarUrls(uiList)
                }
        }
    }

    fun forward(source: Message, targets: List<Conversation>) {
        if (targets.isEmpty()) return
        viewModelScope.launch {
            showLoading(true)
            try {
                forwardMessageUseCase(
                    source = ChatUiMapper.toDomain(source),
                    targets = targets.map { ChatUiMapper.toDomain(it) },
                    timeProvider = { DateUtils.getTimeCurrent() },
                )
                _forwardComplete.value = true
            } catch (e: Exception) {
                showError(e.message.orEmpty())
            } finally {
                showLoading(false)
            }
        }
    }

    fun resetForwardComplete() {
        _forwardComplete.value = false
    }

    private fun syncAvatarUrls(conversations: List<Conversation>) {
        conversations
            .filter { !it.isGroupThread() }
            .map { it.friendId }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { friendId ->
                if (friendId in avatarRequestedIds) return@forEach
                avatarRequestedIds.add(friendId)
                userRepository.getInfoUser(friendId, onSuccess = { user ->
                    if (user.avatar.isNotBlank()) {
                        _avatarMap.update { it + (friendId to user.avatar) }
                    }
                })
            }
    }

    override fun onCleared() {
        inboxJob?.cancel()
        avatarRequestedIds.clear()
        super.onCleared()
    }
}

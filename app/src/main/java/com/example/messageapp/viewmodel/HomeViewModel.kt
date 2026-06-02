package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.PresenceRepository
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.repository.UserRepository
import com.example.messageapp.domain.usecase.inbox.GetUnreadCountUseCase
import com.example.messageapp.domain.usecase.inbox.ObserveInboxUseCase
import com.example.messageapp.mapper.ChatUiMapper
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.UserPresence
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val observeInboxUseCase: ObserveInboxUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val userRepository: UserRepository,
    private val presenceRepository: PresenceRepository,
) : BaseViewModel() {

    /** Session access for fragments/adapters during migration from SharePreferenceRepository. */
    val shared: SessionRepository
        get() = sessionRepository

    private var inboxJob: Job? = null
    private val presenceJobs = mutableMapOf<String, Job>()

    private val _conversation = MutableStateFlow<ArrayList<Conversation>?>(null)
    val conversation = _conversation.asStateFlow()
    private val _numberMsgUnSeen = MutableStateFlow(0)
    val numberMsgUnSeen = _numberMsgUnSeen.asStateFlow()

    private val _presenceMap = MutableStateFlow<Map<String, UserPresence>>(emptyMap())
    val presenceMap = _presenceMap.asStateFlow()

    init {
        viewModelScope.launch {
            conversation.collect { conversations ->
                syncPresenceListeners(conversations.orEmpty())
            }
        }
    }

    fun getListConversation() {
        inboxJob?.cancel()
        inboxJob = viewModelScope.launch {
            observeInboxUseCase()
                .catch { showError(it.message.orEmpty()) }
                .collect { list ->
                    _conversation.value = ArrayList(list.map { ChatUiMapper.toUi(it) })
                }
        }
    }

    fun generateToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            userRepository.saveFcmToken(sessionRepository.getAuth(), token)
        }
    }

    fun getNumberUnSeen() {
        getUnreadCountUseCase { _numberMsgUnSeen.value = it }
    }

    private fun syncPresenceListeners(conversations: List<Conversation>) {
        val friendIds = conversations
            .filter { !it.isGroupThread() }
            .map { it.friendId }
            .filter { it.isNotBlank() }
            .distinct()
            .toSet()

        val removedIds = presenceJobs.keys - friendIds
        removedIds.forEach { friendId ->
            presenceJobs.remove(friendId)?.cancel()
        }
        if (removedIds.isNotEmpty()) {
            _presenceMap.update { current -> current.filterKeys { it in friendIds } }
        }

        friendIds.forEach { friendId ->
            if (friendId in presenceJobs) return@forEach
            presenceJobs[friendId] = viewModelScope.launch {
                presenceRepository.observePresence(friendId).collect { presence ->
                    _presenceMap.update { it + (friendId to ChatUiMapper.toUi(presence)) }
                }
            }
        }
    }

    override fun onCleared() {
        inboxJob?.cancel()
        presenceJobs.values.forEach { it.cancel() }
        presenceJobs.clear()
        super.onCleared()
    }
}

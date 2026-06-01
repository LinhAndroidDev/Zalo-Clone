package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.UserPresence
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.PresenceManager
import com.example.messageapp.utils.SharePreferenceRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : BaseViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository

    @Inject
    lateinit var presenceManager: PresenceManager

    private val _conversation: MutableStateFlow<ArrayList<Conversation>?> = MutableStateFlow(null)
    val conversation = _conversation.asStateFlow()
    private var _numberMsgUnSeen: MutableStateFlow<Int> = MutableStateFlow(0)
    val numberMsgUnSeen = _numberMsgUnSeen.asStateFlow()

    private val _presenceMap = MutableStateFlow<Map<String, UserPresence>>(emptyMap())
    val presenceMap = _presenceMap.asStateFlow()

    private val presenceUnsubscribers = mutableMapOf<String, () -> Unit>()

    init {
        viewModelScope.launch {
            conversation.collect { conversations ->
                syncPresenceListeners(conversations.orEmpty())
            }
        }
    }

    fun getListConversation() = viewModelScope.launch {
        FireBaseInstance.getListConversation(shared.getAuth(),
            success = { result ->
                val conversationData = arrayListOf<Conversation>()
                result?.forEach { document ->
                    val conversation = document.toObject(Conversation::class.java)
                    conversationData.add(conversation)
                }
                _conversation.value = conversationData
            },
            failure = { error ->
                showError(error)
            })
    }

    fun generateToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val hashMap = hashMapOf<String, String>("token" to token)
            FireBaseInstance.saveTokenMessage(shared.getAuth(), hashMap)
        }
    }

    fun getNumberUnSeen() = viewModelScope.launch {
        FireBaseInstance.getNumberUnreadMessages(shared.getAuth()) {
            _numberMsgUnSeen.value = it
        }
    }

    private fun syncPresenceListeners(conversations: List<Conversation>) {
        val friendIds = conversations
            .filter { !it.isGroupThread() }
            .map { it.friendId }
            .filter { it.isNotBlank() }
            .distinct()
            .toSet()

        val removedIds = presenceUnsubscribers.keys - friendIds
        removedIds.forEach { friendId ->
            presenceUnsubscribers.remove(friendId)?.invoke()
        }
        if (removedIds.isNotEmpty()) {
            _presenceMap.update { current -> current.filterKeys { it in friendIds } }
        }

        friendIds.forEach { friendId ->
            if (friendId in presenceUnsubscribers) return@forEach
            val unsubscribe = presenceManager.observePresence(friendId) { presence ->
                _presenceMap.update { it + (friendId to presence) }
            }
            presenceUnsubscribers[friendId] = unsubscribe
        }
    }

    override fun onCleared() {
        presenceUnsubscribers.values.forEach { it.invoke() }
        presenceUnsubscribers.clear()
        super.onCleared()
    }
}

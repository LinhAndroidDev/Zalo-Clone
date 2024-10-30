package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.Message
import com.example.messageapp.model.User
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatFragmentViewModel @Inject constructor() : BaseViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository

    private val _messages: MutableStateFlow<ArrayList<Message>?> = MutableStateFlow(null)
    val messages = _messages.asStateFlow()

    fun sendMessage(
        message: Message,
        time: String,
        friend: User,
    ) = viewModelScope.launch {
        FireBaseInstance.sendMessage(
            message = message,
            keyAuth = shared.getAuth(),
            time = time,
            friend = friend,
            nameSender = shared.getNameUser(),
            avatarSender = shared.getAvatarUser()
        )
    }

    fun getMessage(friendId: String) = viewModelScope.launch {
        val idRoom = listOf(friendId, shared.getAuth()).sorted()
        FireBaseInstance.getMessage(idRoom.toString(),
            success = { result ->
                val messageData = arrayListOf<Message>()
                result?.forEach { document ->
                    val message = document.toObject(Message::class.java)
                    if(isOfThisConversation(message, friendId)) {
                        messageData.add(message)
                    }
                }
                _messages.value = messageData
            },
            failure = { error ->
                showError(error)
            })
    }

    /**
     * This function used to check if the message is from this conversation
     * @param message data receive from FireStore
     * @param friendId key auth of friend
     */
    private fun isOfThisConversation(message: Message, friendId: String): Boolean {
        return message.sender == shared.getAuth() && message.receiver == friendId
                || message.receiver == shared.getAuth() && message.sender == friendId
    }
}
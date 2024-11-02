package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Message
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

    /**
     * This function used to send message to FireStore
     * @param message data message
     * @param time time message sent
     * @param conversation data friend
     */
    fun sendMessage(
        message: Message,
        time: String,
        conversation: Conversation,
    ) = viewModelScope.launch {
        FireBaseInstance.sendMessage(
            message = message,
            userId = shared.getAuth(),
            time = time,
            conversation = conversation,
            nameSender = shared.getNameUser(),
        ) {}
    }

    /** This function used to get message from FireStore
     * @param friendId key auth of friend
     */
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

    fun updateSeenMessage(messages: ArrayList<Message>, conversation: Conversation) {
        if (messages[messages.lastIndex].sender != shared.getAuth()) {
            FireBaseInstance.seenMessage(shared.getAuth(), conversation)
        }
    }
}
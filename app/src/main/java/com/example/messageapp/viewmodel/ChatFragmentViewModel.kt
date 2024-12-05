package com.example.messageapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Emotion
import com.example.messageapp.model.Message
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import com.example.messageapp.utils.getImageDimensions
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
                    if (isOfThisConversation(message, friendId)) {
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

    /**
     * This function used to update seen message for conversation friend
     * @param msg data message
     * @param conversation data friend
     */
    fun updateSeenMessage(msg: Message, conversation: Conversation) = viewModelScope.launch {
        if (msg.sender != shared.getAuth()) {
            FireBaseInstance.getConversation(
                friendId = shared.getAuth(),
                userId = conversation.friendId,
                success = { cvt ->
                    if (!cvt.isSeenMessage() && cvt.sender == conversation.friendId) {
                        FireBaseInstance.seenMessage(
                            shared.getAuth(),
                            friendId = conversation.friendId
                        )
                    }
                }
            )
        }
    }

    /**
     * This function used to upload photo to FireStore
     * @param context context
     * @param uri data uri of photo
     * @param conversation data friend
     * @param time time message sent
     */
    fun uploadListPhoto(
        context: Context,
        uris: ArrayList<Uri>,
        conversation: Conversation,
        time: String
    ) {
        FireBaseInstance.uploadListPhoto(
            context = context,
            uris = uris,
            friendId = conversation.friendId,
            userId = shared.getAuth(),
            process = {

            },
            success = { photos ->
                val photosData = if (photos.size > 1) photos else arrayListOf()
                val singlePhoto = arrayListOf<String>()
                var type = TypeMessage.PHOTOS
                if (photos.size == 1) {
                    type = TypeMessage.SINGLE_PHOTO
                    val data = getImageDimensions(context, uris[0])
                    singlePhoto.add(photos[0])
                    singlePhoto.add(data?.first.toString())
                    singlePhoto.add(data?.second.toString())
                }
                val message = Message(
                    receiver = conversation.friendId,
                    sender = shared.getAuth(),
                    time = time,
                    photos = photosData,
                    singlePhoto = singlePhoto,
                    type = type.ordinal
                )
                FireBaseInstance.sendMessage(
                    message = message,
                    userId = shared.getAuth(),
                    time = time,
                    conversation = conversation,
                    nameSender = shared.getNameUser(),
                    type = TypeMessage.PHOTOS,
                ) {}
            }
        )
    }

    /**
     * This function used to remove message
     * @param context context
     * @param uri data uri of photo
     * @param conversation data friend
     * @param time time message sent
     */
    fun removeMessage(conversation: Conversation, time: String) = viewModelScope.launch {
        FireBaseInstance.removeMessage(
            conversation = conversation,
            userId = shared.getAuth(),
            time = time
        )
    }

    /**
     * This function used to release emotion
     * @param time time message sent
     * @param friendId key auth of friend
     * @param data data emotion
     */
    fun releaseEmotion(time: String, friendId: String, data: Emotion) {
        val idRoom = listOf(friendId, shared.getAuth()).sorted().toString()
        FireBaseInstance.releaseEmotion(
            time = time,
            idRoom = idRoom,
            data = data,
        )
    }
}
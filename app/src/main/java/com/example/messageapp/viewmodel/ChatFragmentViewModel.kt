package com.example.messageapp.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Emotion
import com.example.messageapp.model.Message
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.utils.FileUtils
import com.example.messageapp.utils.FileUtils.isVideoUri
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import com.example.messageapp.utils.getImageDimensions
import com.example.messageapp.utils.getVideoDimensions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatFragmentViewModel @Inject constructor() : BaseViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository

    private val _messages: MutableStateFlow<ArrayList<Message>?> = MutableStateFlow(null)
    val messages = _messages.asStateFlow()

    private val _typing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val typing = _typing.asStateFlow()

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
        sendFirst: Boolean
    ) = viewModelScope.launch {
        FireBaseInstance.sendMessage(
            message = message,
            userId = shared.getAuth(),
            time = time,
            conversation = conversation,
            nameSender = shared.getNameUser(),
            sendFirst = sendFirst
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
                    val raw = document.toObject(Message::class.java) ?: return@forEach
                    if (!isOfThisConversation(raw, friendId)) return@forEach
                    val timeResolved = raw.time.ifBlank { document.id }
                    messageData.add(raw.copy(time = timeResolved))
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
     * @param uris data uri of photo
     * @param conversation data friend
     * @param time time message sent
     */
    fun uploadListPhoto(
        context: Context,
        uris: ArrayList<Uri>,
        conversation: Conversation,
        time: String,
        sendFirst: Boolean
    ) {
        val idRoom = listOf(conversation.friendId, shared.getAuth()).sorted()
        val intrinsicByIndex = ArrayList<Pair<Int, Int>?>(uris.size)
        for (uri in uris) {
            val dim = if (context.isVideoUri(uri)) {
                getVideoDimensions(context, uri)
            } else {
                getImageDimensions(context, uri)
            }
            intrinsicByIndex.add(dim)
        }
        FireBaseInstance.uploadListPhoto(
            context = context,
            uris = uris,
            roomId = idRoom,
            process = {},
            success = { uploadedUrls ->
                if (uploadedUrls.size == 1) {
                    val (w, h) = intrinsicByIndex.getOrNull(0) ?: (0 to 0)
                    val message = Message(
                        receiver = conversation.friendId,
                        sender = shared.getAuth(),
                        time = time,
                        photos = arrayListOf(),
                        photoSizes = null,
                        singlePhoto = arrayListOf(
                            uploadedUrls[0],
                            w.toString(),
                            h.toString()
                        ),
                        type = TypeMessage.SINGLE_PHOTO.ordinal
                    )
                    FireBaseInstance.sendMessage(
                        message = message,
                        userId = shared.getAuth(),
                        time = time,
                        conversation = conversation,
                        nameSender = shared.getNameUser(),
                        type = TypeMessage.SINGLE_PHOTO,
                        sendFirst = sendFirst
                    ) {}
                } else {
                    val sizeTokens = ArrayList<String>(uploadedUrls.size)
                    for (i in uploadedUrls.indices) {
                        val dim = intrinsicByIndex.getOrNull(i)
                        sizeTokens.add(
                            if (dim != null) "${dim.first}x${dim.second}" else "0x0"
                        )
                    }

                    val message = Message(
                        receiver = conversation.friendId,
                        sender = shared.getAuth(),
                        time = time,
                        photos = uploadedUrls,
                        photoSizes = sizeTokens,
                        singlePhoto = arrayListOf(),
                        type = TypeMessage.PHOTOS.ordinal
                    )

                    FireBaseInstance.sendMessage(
                        message = message,
                        userId = shared.getAuth(),
                        time = time,
                        conversation = conversation,
                        nameSender = shared.getNameUser(),
                        type = TypeMessage.PHOTOS,
                        sendFirst = sendFirst
                    ) {}
                }
            }
        )
    }

    fun uploadAudio(
        friendId: String,
        uriAudio: Uri,
        time: String,
        conversation: Conversation,
        sendFirst: Boolean
    ) {
        val idRoom = listOf(friendId, shared.getAuth()).sorted()
        FireBaseInstance.uploadAudio(
            roomId = idRoom,
            uriAudio = uriAudio
        ) { audioUrl ->
            val message = Message(
                receiver = friendId,
                sender = shared.getAuth(),
                time = time,
                audio = audioUrl,
                type = TypeMessage.AUDIO.ordinal
            )
            FireBaseInstance.sendMessage(
                message = message,
                userId = shared.getAuth(),
                time = time,
                conversation = conversation,
                nameSender = shared.getNameUser(),
                type = TypeMessage.AUDIO,
                sendFirst = sendFirst
            ) {}
        }
    }

    /**
     * This function used to remove message
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

    /**
     * This function used to save multi photo to gallery
     * @param context context
     * @param photos data photos
     */
    fun saveMultiPhotoWithCombine(context: Context, photos: ArrayList<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val flows = photos.map { photo ->
                    flow<String> { FileUtils.downloadAndSaveImage(context, photo) }
                        .catch { emit("Error: ${it.message}") }
                        .flowOn(Dispatchers.IO)
                }

                combine(flows) { results ->
                    results.toList() // Chuyển các kết quả thành danh sách
                }.collect { _ ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Đã lưu ảnh", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                showError(e.toString())
            }
        }
    }

    fun updateTyping(friendId: String, typing: Boolean) {
        FireBaseInstance.updateTypingMessage(
            friendId = friendId,
            userId = shared.getAuth(),
            typing = typing
        )
    }

    fun checkShowTyping(friendId: String) {
        FireBaseInstance.getConversationRlt(friendId, shared.getAuth()) { cvt ->
            _typing.value = cvt.typing
        }
    }
}
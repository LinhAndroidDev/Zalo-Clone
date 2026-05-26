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
import com.example.messageapp.model.UserPresence
import com.example.messageapp.utils.FileUtils
import com.example.messageapp.utils.FileUtils.isVideoUri
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.PresenceManager
import com.example.messageapp.utils.SharePreferenceRepository
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.getImageDimensions
import com.example.messageapp.utils.getVideoDimensions
import com.google.firebase.firestore.ListenerRegistration
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

    @Inject
    lateinit var presenceManager: PresenceManager

    private var typingListener: ListenerRegistration? = null
    private var presenceUnsubscriber: (() -> Unit)? = null
    private var groupMemberReadListener: ListenerRegistration? = null
    private var groupMemberIds: List<String> = emptyList()

    private val _messages: MutableStateFlow<ArrayList<Message>?> = MutableStateFlow(null)
    val messages = _messages.asStateFlow()

    private val _groupMemberReadMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val groupMemberReadMap = _groupMemberReadMap.asStateFlow()

    private val _groupLastMessageReaders = MutableStateFlow<List<String>>(emptyList())
    val groupLastMessageReaders = _groupLastMessageReaders.asStateFlow()

    private val _typing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val typing = _typing.asStateFlow()

    private val _friendPresence: MutableStateFlow<UserPresence?> = MutableStateFlow(null)
    val friendPresence = _friendPresence.asStateFlow()

    /** null = ẩn; 0f..100f = tiến độ upload Cloudinary (ảnh/video/ghi âm). */
    private val _cloudUploadProgress = MutableStateFlow<Float?>(null)
    val cloudUploadProgress = _cloudUploadProgress.asStateFlow()

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

    fun roomIdListForCloudinary(conversation: Conversation): List<String> =
        if (conversation.isGroupThread()) listOf(conversation.friendId)
        else listOf(conversation.friendId, shared.getAuth()).sorted()

    fun messageReceiverId(conversation: Conversation): String = conversation.friendId

    /** Load messages for 1-1 or group chat. */
    fun getMessage(conversation: Conversation) = viewModelScope.launch {
        val idRoom = FireBaseInstance.messageThreadDocumentId(conversation, shared.getAuth())
        FireBaseInstance.getMessage(
            idRoom,
            success = { result ->
                val messageData = arrayListOf<Message>()
                result?.forEach { document ->
                    val raw = document.toObject(Message::class.java)
                    if (!isMessageInConversation(raw, conversation)) return@forEach
                    val timeResolved = raw.time.ifBlank { document.id }
                    messageData.add(raw.copy(time = timeResolved))
                }
                _messages.value = messageData
                if (conversation.isGroupThread()) {
                    recomputeGroupReaders(messageData)
                }
            },
            failure = { error ->
                showError(error)
            },
        )
    }

    private fun isMessageInConversation(message: Message, conversation: Conversation): Boolean {
        if (conversation.isGroupThread()) {
            return message.receiver == conversation.friendId
        }
        return message.sender == shared.getAuth() && message.receiver == conversation.friendId
            || message.receiver == shared.getAuth() && message.sender == conversation.friendId
    }

    /**
     * This function used to update seen message for conversation friend
     * @param msg data message
     * @param conversation data friend
     */
    fun updateSeenMessage(msg: Message, conversation: Conversation) = viewModelScope.launch {
        if (conversation.isGroupThread()) {
            if (msg.time.isNotBlank()) {
                FireBaseInstance.markGroupMessageRead(
                    userId = shared.getAuth(),
                    groupId = conversation.friendId,
                    lastReadTime = msg.time,
                )
            }
            return@launch
        }
        if (msg.sender != shared.getAuth()) {
            FireBaseInstance.getConversation(
                friendId = shared.getAuth(),
                userId = conversation.friendId,
                success = { cvt ->
                    if (!cvt.isSeenMessage() && cvt.sender == conversation.friendId) {
                        FireBaseInstance.seenMessage(
                            shared.getAuth(),
                            friendId = conversation.friendId,
                        )
                    }
                },
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
        val idRoom = roomIdListForCloudinary(conversation)
        val intrinsicByIndex = ArrayList<Pair<Int, Int>?>(uris.size)
        for (uri in uris) {
            val dim = if (context.isVideoUri(uri)) {
                getVideoDimensions(context, uri)
            } else {
                getImageDimensions(context, uri)
            }
            intrinsicByIndex.add(dim)
        }
        setCloudUploadProgress(0f)
        FireBaseInstance.uploadListPhoto(
            context = context,
            uris = uris,
            roomId = idRoom,
            process = { (_, overall) ->
                setCloudUploadProgress(overall.toFloat().coerceIn(0f, 100f))
            },
            failure = { t ->
                setCloudUploadProgress(null)
                showError(t.message ?: "Gửi file thất bại")
            },
            success = { uploadedUrls ->
                try {
                if (uploadedUrls.size == 1) {
                    val (w, h) = intrinsicByIndex.getOrNull(0) ?: (0 to 0)
                    val message = Message(
                        receiver = messageReceiverId(conversation),
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
                        receiver = messageReceiverId(conversation),
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
                } finally {
                    setCloudUploadProgress(null)
                }
            }
        )
    }

    private fun setCloudUploadProgress(value: Float?) {
        _cloudUploadProgress.value = value
    }

    fun uploadAudio(
        uriAudio: Uri,
        time: String,
        conversation: Conversation,
        sendFirst: Boolean
    ) {
        val idRoom = roomIdListForCloudinary(conversation)
        setCloudUploadProgress(0f)
        FireBaseInstance.uploadAudio(
            roomId = idRoom,
            uriAudio = uriAudio,
            success = { audioUrl ->
                try {
                    val message = Message(
                        receiver = messageReceiverId(conversation),
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
                } finally {
                    setCloudUploadProgress(null)
                }
            },
            process = { p ->
                setCloudUploadProgress(p.toFloat().coerceIn(0f, 100f))
            },
            failure = { t ->
                setCloudUploadProgress(null)
                showError(t.message ?: "Gửi ghi âm thất bại")
            }
        )
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
    fun releaseEmotion(time: String, conversation: Conversation, data: Emotion) {
        val idRoom = FireBaseInstance.messageThreadDocumentId(conversation, shared.getAuth())
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

    fun updateTyping(conversation: Conversation, typing: Boolean) {
        if (conversation.isGroupThread()) {
            FireBaseInstance.updateGroupTyping(conversation.friendId, shared.getAuth(), typing)
        } else {
            FireBaseInstance.updateTypingMessage(
                userId = shared.getAuth(),
                friendId = conversation.friendId,
                typing = typing,
            )
        }
    }

    fun observeTyping(conversation: Conversation) {
        typingListener?.remove()
        typingListener = null
        if (conversation.isGroupThread()) {
            typingListener = FireBaseInstance.observeGroupTyping(
                conversation.friendId,
                shared.getAuth(),
            ) { show -> _typing.value = show }
        } else {
            FireBaseInstance.getConversationRlt(conversation.friendId, shared.getAuth()) { cvt ->
                _typing.value = cvt.typing
            }
        }
    }

    fun startObservingFriendPresence(friendId: String) {
        stopObservingFriendPresence()
        if (friendId.isBlank()) return
        presenceUnsubscriber = presenceManager.observePresence(friendId) { presence ->
            _friendPresence.value = presence
        }
    }

    fun stopObservingFriendPresence() {
        presenceUnsubscriber?.invoke()
        presenceUnsubscriber = null
        _friendPresence.value = null
    }

    fun startGroupReadTracking(groupId: String) {
        if (groupId.isBlank()) return
        stopGroupReadTracking()
        FireBaseInstance.getGroupMemberIds(
            groupId = groupId,
            success = { memberIds ->
                groupMemberIds = memberIds
                recomputeGroupReaders(_messages.value.orEmpty())
            },
        )
        groupMemberReadListener = FireBaseInstance.observeGroupMemberRead(groupId) { readMap ->
            _groupMemberReadMap.value = readMap
            recomputeGroupReaders(_messages.value.orEmpty())
        }
    }

    fun stopGroupReadTracking() {
        groupMemberReadListener?.remove()
        groupMemberReadListener = null
        groupMemberIds = emptyList()
        _groupMemberReadMap.value = emptyMap()
        _groupLastMessageReaders.value = emptyList()
    }

    private fun recomputeGroupReaders(messages: List<Message>) {
        if (groupMemberIds.isEmpty() || messages.isEmpty()) {
            _groupLastMessageReaders.value = emptyList()
            return
        }
        val lastMsg = messages.last()
        if (lastMsg.sender != shared.getAuth()) {
            _groupLastMessageReaders.value = emptyList()
            return
        }
        val lastMsgMillis = DateUtils.parseChatMessageTimeMillis(lastMsg.time) ?: run {
            _groupLastMessageReaders.value = emptyList()
            return
        }
        val readMap = _groupMemberReadMap.value
        val readers = groupMemberIds
            .filter { memberId ->
                memberId != shared.getAuth() &&
                    (DateUtils.parseChatMessageTimeMillis(readMap[memberId].orEmpty()) ?: 0L) >= lastMsgMillis
            }
        _groupLastMessageReaders.value = readers
    }

    override fun onCleared() {
        typingListener?.remove()
        stopObservingFriendPresence()
        stopGroupReadTracking()
        super.onCleared()
    }
}
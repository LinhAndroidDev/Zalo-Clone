package com.example.messageapp.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.data.legacy.DateUtils
import com.example.messageapp.domain.repository.FriendRepository
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.repository.UserRepository
import com.example.messageapp.domain.usecase.inbox.GetConversationUseCase
import com.example.messageapp.domain.usecase.chat.AddGroupMembersUseCase
import com.example.messageapp.domain.usecase.chat.LeaveGroupUseCase
import com.example.messageapp.domain.usecase.chat.LoadGroupMembersUseCase
import com.example.messageapp.domain.usecase.chat.RemoveGroupMemberUseCase
import com.example.messageapp.domain.usecase.chat.MarkMessageReadUseCase
import com.example.messageapp.domain.usecase.chat.ObserveGroupReadStatusUseCase
import com.example.messageapp.domain.usecase.chat.ObserveMessagesUseCase
import com.example.messageapp.domain.usecase.chat.ObservePresenceUseCase
import com.example.messageapp.domain.usecase.chat.ObserveTypingUseCase
import com.example.messageapp.domain.usecase.chat.RemoveMessageUseCase
import com.example.messageapp.domain.usecase.chat.SendMessageUseCase
import com.example.messageapp.domain.usecase.chat.ToggleMessageReactionUseCase
import com.example.messageapp.domain.usecase.chat.UpdateTypingUseCase
import com.example.messageapp.domain.usecase.chat.UploadChatMediaUseCase
import com.example.messageapp.mapper.ChatUiMapper
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.EmotionType
import com.example.messageapp.model.Friend
import com.example.messageapp.model.Message
import com.example.messageapp.model.User
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.model.UserPresence
import com.example.messageapp.utils.FileUtils
import com.example.messageapp.utils.FileUtils.isVideoUri
import com.example.messageapp.utils.GroupAvatarLoader
import com.example.messageapp.utils.MentionHelper
import com.example.messageapp.utils.getImageDimensions
import com.example.messageapp.utils.getVideoDimensions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
class ChatFragmentViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getConversationUseCase: GetConversationUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val toggleMessageReactionUseCase: ToggleMessageReactionUseCase,
    private val observeTypingUseCase: ObserveTypingUseCase,
    private val updateTypingUseCase: UpdateTypingUseCase,
    private val markMessageReadUseCase: MarkMessageReadUseCase,
    private val removeMessageUseCase: RemoveMessageUseCase,
    private val observePresenceUseCase: ObservePresenceUseCase,
    private val observeGroupReadStatusUseCase: ObserveGroupReadStatusUseCase,
    private val loadGroupMembersUseCase: LoadGroupMembersUseCase,
    private val uploadChatMediaUseCase: UploadChatMediaUseCase,
    private val userRepository: UserRepository,
    private val friendRepository: FriendRepository,
    private val addGroupMembersUseCase: AddGroupMembersUseCase,
    private val removeGroupMemberUseCase: RemoveGroupMemberUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val groupAvatarLoader: GroupAvatarLoader,
) : BaseViewModel() {

    private fun refreshGroupAvatarCache(groupId: String) {
        groupAvatarLoader.invalidate(groupId)
    }

    /** Session access for fragments/adapters during migration from SharePreferenceRepository. */
    val shared: SessionRepository
        get() = sessionRepository

    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    private var presenceJob: Job? = null
    private var groupReadJob: Job? = null
    private var peerConversationJob: Job? = null
    private var groupMemberIds: List<String> = emptyList()

    private val _messages = MutableStateFlow<ArrayList<Message>?>(null)
    val messages = _messages.asStateFlow()

    private val _groupMemberReadMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val groupMemberReadMap = _groupMemberReadMap.asStateFlow()

    private val _groupLastMessageReaders = MutableStateFlow<List<String>>(emptyList())
    val groupLastMessageReaders = _groupLastMessageReaders.asStateFlow()

    private val _typing = MutableStateFlow(false)
    val typing = _typing.asStateFlow()

    private val _friendPresence = MutableStateFlow<UserPresence?>(null)
    val friendPresence = _friendPresence.asStateFlow()

    private val _cloudUploadProgress = MutableStateFlow<Float?>(null)
    val cloudUploadProgress = _cloudUploadProgress.asStateFlow()

    private val _mentionCandidates = MutableStateFlow<List<MentionHelper.MentionCandidate>>(emptyList())
    val mentionCandidates = _mentionCandidates.asStateFlow()

    private val _peerConversation = MutableStateFlow<Conversation?>(null)
    val peerConversation = _peerConversation.asStateFlow()

    private val _friendsToAdd = MutableStateFlow<List<Friend>>(emptyList())
    val friendsToAdd = _friendsToAdd.asStateFlow()

    private val _groupMembersForManage = MutableStateFlow<List<User>>(emptyList())
    val groupMembersForManage = _groupMembersForManage.asStateFlow()

    fun sendMessage(message: Message, time: String, conversation: Conversation, sendFirst: Boolean) {
        sendMessageUseCase(ChatUiMapper.toDomain(message), time, ChatUiMapper.toDomain(conversation), sendFirst)
    }

    fun roomIdListForCloudinary(conversation: Conversation): List<String> =
        if (conversation.isGroupThread()) listOf(conversation.friendId)
        else listOf(conversation.friendId, sessionRepository.getAuth()).sorted()

    fun messageReceiverId(conversation: Conversation): String = conversation.friendId

    fun getMessage(conversation: Conversation) {
        messagesJob?.cancel()
        val domainConversation = ChatUiMapper.toDomain(conversation)
        messagesJob = viewModelScope.launch {
            observeMessagesUseCase(domainConversation)
                .catch { showError(it.message.orEmpty()) }
                .collect { domainMessages ->
                    val uiMessages = ChatUiMapper.toUiList(domainMessages)
                    _messages.value = uiMessages
                    if (conversation.isGroupThread()) {
                        recomputeGroupReaders(uiMessages)
                    }
                }
        }
    }

    fun updateSeenMessage(msg: Message, conversation: Conversation) {
        markMessageReadUseCase(ChatUiMapper.toDomain(msg), ChatUiMapper.toDomain(conversation))
    }

    fun uploadListPhoto(
        context: Context,
        uris: ArrayList<Uri>,
        conversation: Conversation,
        time: String,
        sendFirst: Boolean,
    ) {
        val idRoom = roomIdListForCloudinary(conversation)
        val intrinsicByIndex = ArrayList<Pair<Int, Int>?>(uris.size)
        for (uri in uris) {
            val dim = if (context.isVideoUri(uri)) getVideoDimensions(context, uri)
            else getImageDimensions(context, uri)
            intrinsicByIndex.add(dim)
        }
        setCloudUploadProgress(0f)
        uploadChatMediaUseCase.uploadPhotos(
            uriStrings = uris.map { it.toString() },
            roomId = idRoom,
            onProgress = { setCloudUploadProgress(it.toFloat().coerceIn(0f, 100f)) },
            onFailure = { t ->
                setCloudUploadProgress(null)
                showError(t.message ?: "Gửi file thất bại")
            },
            onSuccess = { uploadedUrls ->
                try {
                    if (uploadedUrls.size == 1) {
                        val (w, h) = intrinsicByIndex.getOrNull(0) ?: (0 to 0)
                        val message = Message(
                            receiver = messageReceiverId(conversation),
                            sender = sessionRepository.getAuth(),
                            time = time,
                            singlePhoto = arrayListOf(uploadedUrls[0], w.toString(), h.toString()),
                            type = TypeMessage.SINGLE_PHOTO.ordinal,
                        )
                        sendMessage(message, time, conversation, sendFirst)
                    } else {
                        val sizeTokens = ArrayList<String>(uploadedUrls.size)
                        for (i in uploadedUrls.indices) {
                            val dim = intrinsicByIndex.getOrNull(i)
                            sizeTokens.add(if (dim != null) "${dim.first}x${dim.second}" else "0x0")
                        }
                        val message = Message(
                            receiver = messageReceiverId(conversation),
                            sender = sessionRepository.getAuth(),
                            time = time,
                            photos = ArrayList(uploadedUrls),
                            photoSizes = sizeTokens,
                            type = TypeMessage.PHOTOS.ordinal,
                        )
                        sendMessage(message, time, conversation, sendFirst)
                    }
                } finally {
                    setCloudUploadProgress(null)
                }
            },
        )
    }

    private fun setCloudUploadProgress(value: Float?) {
        _cloudUploadProgress.value = value
    }

    fun uploadAudio(uriAudio: Uri, time: String, conversation: Conversation, sendFirst: Boolean) {
        val idRoom = roomIdListForCloudinary(conversation)
        setCloudUploadProgress(0f)
        uploadChatMediaUseCase.uploadAudio(
            uriString = uriAudio.toString(),
            roomId = idRoom,
            onProgress = { setCloudUploadProgress(it.toFloat().coerceIn(0f, 100f)) },
            onSuccess = { audioUrl ->
                try {
                    val message = Message(
                        receiver = messageReceiverId(conversation),
                        sender = sessionRepository.getAuth(),
                        time = time,
                        audio = audioUrl,
                        type = TypeMessage.AUDIO.ordinal,
                    )
                    sendMessage(message, time, conversation, sendFirst)
                } finally {
                    setCloudUploadProgress(null)
                }
            },
            onFailure = { t ->
                setCloudUploadProgress(null)
                showError(t.message ?: "Gửi ghi âm thất bại")
            },
        )
    }

    fun removeMessage(conversation: Conversation, time: String) {
        removeMessageUseCase(ChatUiMapper.toDomain(conversation), time)
    }

    fun toggleMessageReaction(
        time: String,
        conversation: Conversation,
        type: EmotionType,
        onApplied: ((EmotionType) -> Unit)? = null,
    ) {
        val currentList = _messages.value ?: return
        val domainList = currentList.map { ChatUiMapper.toDomain(it) }
        val previousEmotion = currentList.firstOrNull { it.time == time }?.emotion
        val result = toggleMessageReactionUseCase(
            messages = domainList,
            time = time,
            conversation = ChatUiMapper.toDomain(conversation),
            type = ChatUiMapper.toDomain(type),
            onFailure = { error ->
                val rollbackList = ArrayList(_messages.value.orEmpty())
                val rollbackIndex = rollbackList.indexOfFirst { it.time == time }
                if (rollbackIndex >= 0) {
                    rollbackList[rollbackIndex] = rollbackList[rollbackIndex].copy(emotion = previousEmotion)
                    _messages.value = rollbackList
                }
                showError(error)
            },
        ) ?: return
        _messages.value = ArrayList(result.optimisticMessages.map { ChatUiMapper.toUi(it) })
        if (result.reactionApplied) {
            onApplied?.invoke(type)
        }
    }

    fun saveMultiPhotoWithCombine(context: Context, photos: ArrayList<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val flows = photos.map { photo ->
                    flow<String> { FileUtils.downloadAndSaveImage(context, photo) }
                        .catch { emit("Error: ${it.message}") }
                        .flowOn(Dispatchers.IO)
                }
                combine(flows) { it.toList() }.collect {
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
        updateTypingUseCase(ChatUiMapper.toDomain(conversation), typing)
    }

    fun observeTyping(conversation: Conversation) {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            observeTypingUseCase(ChatUiMapper.toDomain(conversation)).collect { _typing.value = it }
        }
    }

    fun startObservingFriendPresence(friendId: String) {
        stopObservingFriendPresence()
        if (friendId.isBlank()) return
        presenceJob = viewModelScope.launch {
            observePresenceUseCase(friendId).collect { presence ->
                _friendPresence.value = ChatUiMapper.toUi(presence)
            }
        }
    }

    fun stopObservingFriendPresence() {
        presenceJob?.cancel()
        presenceJob = null
        _friendPresence.value = null
    }

    fun startObservingPeerConversation(friendId: String) {
        stopObservingPeerConversation()
        if (friendId.isBlank()) return
        peerConversationJob = viewModelScope.launch {
            getConversationUseCase.observe(friendId, sessionRepository.getAuth())
                .catch { showError(it.message.orEmpty()) }
                .collect { conversation ->
                    _peerConversation.value = ChatUiMapper.toUi(conversation)
                }
        }
    }

    fun stopObservingPeerConversation() {
        peerConversationJob?.cancel()
        peerConversationJob = null
        _peerConversation.value = null
    }

    fun startGroupReadTracking(groupId: String) {
        if (groupId.isBlank()) return
        stopGroupReadTracking()
        loadGroupMentionMembers(groupId)
        loadGroupMembersUseCase(
            groupId = groupId,
            onSuccess = { users ->
                groupMemberIds = users.map { it.keyAuth }
                recomputeGroupReaders(_messages.value.orEmpty())
            },
        )
        groupReadJob = viewModelScope.launch {
            observeGroupReadStatusUseCase(groupId).collect { readMap ->
                _groupMemberReadMap.value = readMap
                recomputeGroupReaders(_messages.value.orEmpty())
            }
        }
    }

    fun stopGroupReadTracking() {
        groupReadJob?.cancel()
        groupReadJob = null
        groupMemberIds = emptyList()
        _groupMemberReadMap.value = emptyMap()
        _groupLastMessageReaders.value = emptyList()
        _mentionCandidates.value = emptyList()
    }

    fun loadGroupMentionMembers(groupId: String) {
        if (groupId.isBlank()) {
            _mentionCandidates.value = emptyList()
            return
        }
        loadGroupMembersUseCase(
            groupId = groupId,
            onSuccess = { users ->
                val uiUsers = users.map { ChatUiMapper.toUi(it) }
                _mentionCandidates.value = MentionHelper.buildMentionCandidates(uiUsers)
            },
            onFailure = { _mentionCandidates.value = emptyList() },
        )
    }

    private fun recomputeGroupReaders(messages: List<Message>) {
        if (groupMemberIds.isEmpty() || messages.isEmpty()) {
            _groupLastMessageReaders.value = emptyList()
            return
        }
        val lastMsg = messages.last()
        if (lastMsg.sender != sessionRepository.getAuth()) {
            _groupLastMessageReaders.value = emptyList()
            return
        }
        val lastMsgMillis = DateUtils.parseChatMessageTimeMillis(lastMsg.time) ?: run {
            _groupLastMessageReaders.value = emptyList()
            return
        }
        val readMap = _groupMemberReadMap.value
        val readers = groupMemberIds.filter { memberId ->
            memberId != sessionRepository.getAuth() &&
                (DateUtils.parseChatMessageTimeMillis(readMap[memberId].orEmpty()) ?: 0L) >= lastMsgMillis
        }
        _groupLastMessageReaders.value = readers
    }

    fun loadUserAvatar(userId: String, onResult: (String) -> Unit) {
        userRepository.getInfoUser(userId, onSuccess = { onResult(it.avatar) })
    }

    fun prepareAddMembersSheet(groupId: String) {
        friendRepository.getFriends(
            userId = sessionRepository.getAuth(),
            onSuccess = { friends ->
                loadGroupMembersUseCase(
                    groupId = groupId,
                    onSuccess = { members ->
                        val memberIds = members.map { it.keyAuth }.toSet()
                        _friendsToAdd.value = friends
                            .map { ChatUiMapper.toUi(it) }
                            .filter { it.keyAuth.isNotBlank() && it.keyAuth !in memberIds }
                    },
                    onFailure = {
                        _friendsToAdd.value = friends.map { ChatUiMapper.toUi(it) }
                    },
                )
            },
            onFailure = { showError(it) },
        )
    }

    fun refreshGroupMembersForManage(groupId: String) {
        loadGroupMembersUseCase(
            groupId = groupId,
            onSuccess = { users ->
                _groupMembersForManage.value = users.map { ChatUiMapper.toUi(it) }
                loadGroupMentionMembers(groupId)
            },
            onFailure = { showError(it) },
        )
    }

    fun addGroupMembers(groupId: String, memberIds: List<String>, onSuccess: () -> Unit) {
        addGroupMembersUseCase(
            groupId = groupId,
            newMemberIds = memberIds,
            onSuccess = {
                refreshGroupAvatarCache(groupId)
                showMessage("Đã thêm thành viên vào nhóm")
                refreshGroupMembersForManage(groupId)
                onSuccess()
            },
            onFailure = { showError(it) },
        )
    }

    fun removeGroupMember(groupId: String, memberId: String, onSuccess: () -> Unit) {
        removeGroupMemberUseCase(
            groupId = groupId,
            memberId = memberId,
            onSuccess = {
                refreshGroupAvatarCache(groupId)
                showMessage("Đã xóa thành viên khỏi nhóm")
                refreshGroupMembersForManage(groupId)
                onSuccess()
            },
            onFailure = { showError(it) },
        )
    }

    fun leaveGroup(groupId: String, onSuccess: () -> Unit) {
        leaveGroupUseCase(
            groupId = groupId,
            onSuccess = {
                refreshGroupAvatarCache(groupId)
                showMessage("Đã rời nhóm")
                onSuccess()
            },
            onFailure = { showError(it) },
        )
    }

    override fun onCleared() {
        messagesJob?.cancel()
        typingJob?.cancel()
        stopObservingFriendPresence()
        stopObservingPeerConversation()
        stopGroupReadTracking()
        super.onCleared()
    }
}

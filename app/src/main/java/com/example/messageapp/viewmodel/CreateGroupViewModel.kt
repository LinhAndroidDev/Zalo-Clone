package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.FriendRepository
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.chat.CreateGroupUseCase
import com.example.messageapp.mapper.ChatUiMapper
import com.example.messageapp.model.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val friendRepository: FriendRepository,
    private val createGroupUseCase: CreateGroupUseCase,
) : BaseViewModel() {

    private val _friends = MutableStateFlow<List<com.example.messageapp.model.Friend>>(emptyList())
    val friends: StateFlow<List<com.example.messageapp.model.Friend>> = _friends.asStateFlow()

    fun loadFriends() = viewModelScope.launch {
        friendRepository.getFriends(
            userId = sessionRepository.getAuth(),
            onSuccess = { list -> _friends.value = list.map { ChatUiMapper.toUi(it) } },
            onFailure = { showError(it) },
        )
    }

    fun createGroup(
        displayName: String,
        welcomeMessage: String,
        welcomeInboxPerson: String,
        otherMemberIds: List<String>,
        onSuccess: (Conversation) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        createGroupUseCase(
            name = displayName,
            creatorId = sessionRepository.getAuth(),
            creatorAvatar = "",
            welcomeMessage = welcomeMessage,
            welcomeInboxPerson = welcomeInboxPerson,
            memberIds = otherMemberIds,
            onSuccess = { _, inbox -> onSuccess(ChatUiMapper.toUi(inbox)) },
            onFailure = onFailure,
        )
    }
}

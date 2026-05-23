package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Friend
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor() : BaseViewModel() {

    @Inject
    lateinit var shared: SharePreferenceRepository

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    fun loadFriends() = viewModelScope.launch {
        FireBaseInstance.getFriends(
            userId = shared.getAuth(),
            success = { list -> _friends.value = list },
            failure = { showError(it) },
        )
    }

    fun createGroup(
        displayName: String,
        otherMemberIds: List<String>,
        onSuccess: (Conversation) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.createGroup(
            name = displayName,
            creatorId = shared.getAuth(),
            creatorAvatar = "",
            otherMemberIds = otherMemberIds,
            success = { _, inbox -> onSuccess(inbox) },
            failure = onFailure,
        )
    }
}

package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.FriendRequest
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FragmentFriendRequestViewModel @Inject constructor() : BaseViewModel() {

    @Inject
    lateinit var shared: SharePreferenceRepository

    private val _receivedRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val receivedRequests = _receivedRequests.asStateFlow()

    private val _sentRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val sentRequests = _sentRequests.asStateFlow()

    fun getIncomingFriendRequests() = viewModelScope.launch {
        FireBaseInstance.getIncomingFriendRequests(
            userId = shared.getAuth(),
            success = { _receivedRequests.value = it },
            failure = { showError(it) }
        )
    }

    fun getOutgoingFriendRequests() = viewModelScope.launch {
        FireBaseInstance.getOutgoingFriendRequests(
            userId = shared.getAuth(),
            success = { _sentRequests.value = it },
            failure = { showError(it) }
        )
    }

    fun acceptRequest(request: FriendRequest) = viewModelScope.launch {
        // One-shot fetch avoids getInfoUser snapshot firing multiple times and keeps
        // myName/myAvatar reliable as fallback when request.toName/toAvatar are blank (old data).
        FireBaseInstance.getUserById(
            userId = shared.getAuth(),
            success = { me ->
                FireBaseInstance.acceptFriendRequest(
                    request = request,
                    myName = me.name.orEmpty(),
                    myAvatar = me.avatar.orEmpty(),
                    success = { showMessage("Đã chấp nhận lời mời kết bạn") },
                    failure = { showError(it) }
                )
            },
            failure = { showError(it) }
        )
    }

    fun rejectRequest(requestId: String) = viewModelScope.launch {
        FireBaseInstance.rejectFriendRequest(
            requestId = requestId,
            success = { showMessage("Đã từ chối lời mời kết bạn") },
            failure = { showError(it) }
        )
    }

    fun cancelSentRequest(request: FriendRequest) = viewModelScope.launch {
        FireBaseInstance.cancelFriendRequest(
            fromId = shared.getAuth(),
            toId = request.toId,
            success = { showMessage("Đã huỷ lời mời kết bạn") },
            failure = { showError(it) }
        )
    }
}

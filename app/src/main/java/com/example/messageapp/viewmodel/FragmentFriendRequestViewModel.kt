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

    private val _requests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val requests = _requests.asStateFlow()

    fun getIncomingFriendRequests() = viewModelScope.launch {
        FireBaseInstance.getIncomingFriendRequests(
            userId = shared.getAuth(),
            success = { _requests.value = it },
            failure = { showError(it) }
        )
    }

    fun acceptRequest(request: FriendRequest) = viewModelScope.launch {
        FireBaseInstance.getInfoUser(shared.getAuth()) { me ->
            FireBaseInstance.acceptFriendRequest(
                request = request,
                myName = me.name.orEmpty(),
                myAvatar = me.avatar.orEmpty(),
                success = { showMessage("Đã chấp nhận lời mời kết bạn") },
                failure = { showError(it) }
            )
        }
    }

    fun rejectRequest(requestId: String) = viewModelScope.launch {
        FireBaseInstance.rejectFriendRequest(
            requestId = requestId,
            success = { showMessage("Đã từ chối lời mời kết bạn") },
            failure = { showError(it) }
        )
    }
}

package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.usecase.social.AcceptFriendRequestUseCase
import com.example.messageapp.domain.usecase.social.CancelFriendRequestUseCase
import com.example.messageapp.domain.usecase.social.GetIncomingFriendRequestsUseCase
import com.example.messageapp.domain.usecase.social.GetOutgoingFriendRequestsUseCase
import com.example.messageapp.domain.usecase.social.RejectFriendRequestUseCase
import com.example.messageapp.mapper.SocialUiMapper
import com.example.messageapp.model.FriendRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FragmentFriendRequestViewModel @Inject constructor(
    private val getIncomingFriendRequestsUseCase: GetIncomingFriendRequestsUseCase,
    private val getOutgoingFriendRequestsUseCase: GetOutgoingFriendRequestsUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val rejectFriendRequestUseCase: RejectFriendRequestUseCase,
    private val cancelFriendRequestUseCase: CancelFriendRequestUseCase,
) : BaseViewModel() {

    private val _receivedRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val receivedRequests = _receivedRequests.asStateFlow()

    private val _sentRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val sentRequests = _sentRequests.asStateFlow()

    fun getIncomingFriendRequests() = viewModelScope.launch {
        getIncomingFriendRequestsUseCase(
            onSuccess = { _receivedRequests.value = SocialUiMapper.toUiRequests(it) },
            onFailure = { showError(it) },
        )
    }

    fun getOutgoingFriendRequests() = viewModelScope.launch {
        getOutgoingFriendRequestsUseCase(
            onSuccess = { _sentRequests.value = SocialUiMapper.toUiRequests(it) },
            onFailure = { showError(it) },
        )
    }

    fun acceptRequest(request: FriendRequest) = viewModelScope.launch {
        acceptFriendRequestUseCase(
            request = SocialUiMapper.toDomain(request),
            onSuccess = { showMessage("Đã chấp nhận lời mời kết bạn") },
            onFailure = { showError(it) },
        )
    }

    fun rejectRequest(requestId: String) = viewModelScope.launch {
        rejectFriendRequestUseCase(
            requestId = requestId,
            onSuccess = { showMessage("Đã từ chối lời mời kết bạn") },
            onFailure = { showError(it) },
        )
    }

    fun cancelSentRequest(request: FriendRequest) = viewModelScope.launch {
        cancelFriendRequestUseCase(
            toId = request.toId,
            onSuccess = { showMessage("Đã huỷ lời mời kết bạn") },
            onFailure = { showError(it) },
        )
    }
}

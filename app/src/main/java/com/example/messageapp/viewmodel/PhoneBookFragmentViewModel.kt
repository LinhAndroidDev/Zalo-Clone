package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.usecase.social.GetFriendsUseCase
import com.example.messageapp.domain.usecase.social.GetIncomingFriendRequestsUseCase
import com.example.messageapp.mapper.SocialUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneBookFragmentViewModel @Inject constructor(
    private val getFriendsUseCase: GetFriendsUseCase,
    private val getIncomingFriendRequestsUseCase: GetIncomingFriendRequestsUseCase,
) : BaseViewModel() {

    private val _friends = MutableStateFlow<List<com.example.messageapp.model.Friend>>(emptyList())
    val friends = _friends.asStateFlow()

    private val _totalRequestCount = MutableStateFlow(0)
    val totalRequestCount = _totalRequestCount.asStateFlow()

    fun getFriends() = viewModelScope.launch {
        getFriendsUseCase(
            onSuccess = { _friends.value = SocialUiMapper.toUiFriends(it) },
            onFailure = { showError(it) },
        )
    }

    fun getPendingRequestCounts() = viewModelScope.launch {
        getIncomingFriendRequestsUseCase(
            onSuccess = { requests -> _totalRequestCount.value = requests.size },
            onFailure = { showError(it) },
        )
    }
}

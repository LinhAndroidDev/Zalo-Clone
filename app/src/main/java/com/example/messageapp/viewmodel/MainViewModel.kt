package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.social.GetIncomingFriendRequestsUseCase
import com.example.messageapp.mapper.SocialUiMapper
import com.example.messageapp.model.FriendRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getIncomingFriendRequestsUseCase: GetIncomingFriendRequestsUseCase,
) : BaseViewModel() {

    private val _latestRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    private val _lastSeenAt = MutableStateFlow(0L)

    val newFriendRequestCount: StateFlow<Int> = combine(_latestRequests, _lastSeenAt) { requests, seenAt ->
        requests.count { it.createdAt > seenAt }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun startListening() = viewModelScope.launch {
        _lastSeenAt.value = sessionRepository.getLastSeenFriendRequestAt()
        getIncomingFriendRequestsUseCase(
            onSuccess = { _latestRequests.value = SocialUiMapper.toUiRequests(it) },
            onFailure = {},
        )
    }

    fun markAsSeen() {
        val now = System.currentTimeMillis()
        sessionRepository.saveLastSeenFriendRequestAt(now)
        _lastSeenAt.value = now
    }
}

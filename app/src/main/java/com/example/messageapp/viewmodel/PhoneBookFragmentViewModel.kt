package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.PresenceRepository
import com.example.messageapp.domain.usecase.social.GetFriendsUseCase
import com.example.messageapp.domain.usecase.social.GetIncomingFriendRequestsUseCase
import com.example.messageapp.mapper.ChatUiMapper
import com.example.messageapp.mapper.SocialUiMapper
import com.example.messageapp.model.UserPresence
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneBookFragmentViewModel @Inject constructor(
    private val getFriendsUseCase: GetFriendsUseCase,
    private val getIncomingFriendRequestsUseCase: GetIncomingFriendRequestsUseCase,
    private val presenceRepository: PresenceRepository,
) : BaseViewModel() {

    private val presenceJobs = mutableMapOf<String, Job>()

    private val _friends = MutableStateFlow<List<com.example.messageapp.model.Friend>>(emptyList())
    val friends = _friends.asStateFlow()

    private val _totalRequestCount = MutableStateFlow(0)
    val totalRequestCount = _totalRequestCount.asStateFlow()

    private val _presenceMap = MutableStateFlow<Map<String, UserPresence>>(emptyMap())
    val presenceMap = _presenceMap.asStateFlow()

    fun getFriends() = viewModelScope.launch {
        getFriendsUseCase(
            onSuccess = { friends ->
                _friends.value = SocialUiMapper.toUiFriends(friends)
                syncPresenceListeners(_friends.value)
            },
            onFailure = { showError(it) },
        )
    }

    fun getPendingRequestCounts() = viewModelScope.launch {
        getIncomingFriendRequestsUseCase(
            onSuccess = { requests -> _totalRequestCount.value = requests.size },
            onFailure = { showError(it) },
        )
    }

    private fun syncPresenceListeners(friends: List<com.example.messageapp.model.Friend>) {
        val friendIds = friends
            .map { it.keyAuth }
            .filter { it.isNotBlank() }
            .distinct()
            .toSet()

        val removedIds = presenceJobs.keys - friendIds
        removedIds.forEach { friendId ->
            presenceJobs.remove(friendId)?.cancel()
        }
        if (removedIds.isNotEmpty()) {
            _presenceMap.update { current -> current.filterKeys { it in friendIds } }
        }

        friendIds.forEach { friendId ->
            if (friendId in presenceJobs) return@forEach
            presenceJobs[friendId] = viewModelScope.launch {
                presenceRepository.observePresence(friendId).collect { presence ->
                    _presenceMap.update { it + (friendId to ChatUiMapper.toUi(presence)) }
                }
            }
        }
    }

    override fun onCleared() {
        presenceJobs.values.forEach { it.cancel() }
        presenceJobs.clear()
        super.onCleared()
    }
}

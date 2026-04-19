package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.FriendRequest
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : BaseViewModel() {

    @Inject
    lateinit var shared: SharePreferenceRepository

    private val _latestRequests = MutableStateFlow<List<FriendRequest>>(emptyList())

    /** Reactive "last seen" timestamp — updates immediately when user opens FriendRequestFragment */
    private val _lastSeenAt = MutableStateFlow(0L)

    /**
     * Real-time badge count: requests with createdAt newer than lastSeenAt.
     * Recalculates automatically whenever either the requests list or lastSeenAt changes.
     */
    val newFriendRequestCount: StateFlow<Int> = combine(_latestRequests, _lastSeenAt) { requests, seenAt ->
        requests.count { it.createdAt > seenAt }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun startListening() = viewModelScope.launch {
        _lastSeenAt.value = shared.getLastSeenFriendRequestAt()
        FireBaseInstance.getIncomingFriendRequests(
            userId = shared.getAuth(),
            success = { _latestRequests.value = it },
            failure = {}
        )
    }

    fun markAsSeen() {
        val now = System.currentTimeMillis()
        shared.saveLastSeenFriendRequestAt(now)
        _lastSeenAt.value = now
    }
}

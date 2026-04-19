package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.User
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserWithStatus(val user: User, val status: String)

@HiltViewModel
class SearchFragmentViewModel @Inject constructor() : BaseViewModel() {

    @Inject
    lateinit var shared: SharePreferenceRepository

    private val _users: MutableStateFlow<List<UserWithStatus>> = MutableStateFlow(emptyList())
    val users = _users.asStateFlow()

    private val _history: MutableStateFlow<List<User>> = MutableStateFlow(emptyList())
    val history = _history.asStateFlow()

    // Tracks the most recent query to discard stale async callbacks
    @Volatile private var currentQuery: String = ""

    fun searchFriend(keySearch: String) {
        currentQuery = keySearch
        if (keySearch.isBlank()) {
            _users.value = emptyList()
            getSearchHistory()
            return
        }
        FireBaseInstance.searchFriend(queryText = keySearch) { results ->
            // Discard results if a newer query has already been issued
            if (keySearch != currentQuery) return@searchFriend

            val myId = shared.getAuth()
            val others = results.filter { it.keyAuth != myId }
            if (others.isEmpty()) {
                if (keySearch == currentQuery) _users.value = emptyList()
                return@searchFriend
            }
            val output = mutableListOf<UserWithStatus>()
            var pending = others.size
            others.forEach { user ->
                FireBaseInstance.getFriendshipStatus(myId, user.keyAuth.orEmpty()) { status ->
                    synchronized(output) {
                        output.add(UserWithStatus(user, status))
                        pending--
                        if (pending == 0 && keySearch == currentQuery) {
                            _users.value = output.sortedBy { it.user.name }
                        }
                    }
                }
            }
        }
    }

    fun sendFriendRequest(target: User) = viewModelScope.launch {
        FireBaseInstance.getInfoUser(shared.getAuth()) { me ->
            FireBaseInstance.sendFriendRequest(
                fromId = shared.getAuth(),
                fromName = me.name.orEmpty(),
                fromAvatar = me.avatar.orEmpty(),
                toId = target.keyAuth.orEmpty(),
                success = { showMessage("Đã gửi lời mời kết bạn") },
                failure = { showError(it) }
            )
        }
    }

    fun getSearchHistory() = viewModelScope.launch {
        FireBaseInstance.getSearchHistory(
            myId = shared.getAuth(),
            success = { _history.value = it },
            failure = { showError(it) }
        )
    }

    fun saveSearchHistory(user: User) = viewModelScope.launch {
        FireBaseInstance.saveSearchHistory(
            myId = shared.getAuth(),
            user = user,
            failure = { showError(it) }
        )
    }
}

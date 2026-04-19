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

    fun searchFriend(keySearch: String) {
        FireBaseInstance.searchFriend(queryText = keySearch) { results ->
            val myId = shared.getAuth()
            // Filter out self
            val others = results.filter { it.keyAuth != myId }
            if (others.isEmpty()) {
                _users.value = emptyList()
                return@searchFriend
            }
            val output = mutableListOf<UserWithStatus>()
            var pending = others.size
            others.forEach { user ->
                FireBaseInstance.getFriendshipStatus(myId, user.keyAuth.orEmpty()) { status ->
                    synchronized(output) {
                        output.add(UserWithStatus(user, status))
                        pending--
                        if (pending == 0) {
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
}

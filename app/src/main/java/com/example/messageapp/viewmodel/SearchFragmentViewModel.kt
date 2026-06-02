package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.social.CancelFriendRequestUseCase
import com.example.messageapp.domain.usecase.social.GetFriendshipStatusUseCase
import com.example.messageapp.domain.usecase.social.GetSearchHistoryUseCase
import com.example.messageapp.domain.usecase.social.SaveSearchHistoryUseCase
import com.example.messageapp.domain.usecase.social.SearchUsersUseCase
import com.example.messageapp.domain.usecase.social.SendFriendRequestUseCase
import com.example.messageapp.mapper.SocialUiMapper
import com.example.messageapp.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserWithStatus(val user: User, val status: String)

@HiltViewModel
class SearchFragmentViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val getFriendshipStatusUseCase: GetFriendshipStatusUseCase,
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
    private val cancelFriendRequestUseCase: CancelFriendRequestUseCase,
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val saveSearchHistoryUseCase: SaveSearchHistoryUseCase,
) : BaseViewModel() {

    private val _users = MutableStateFlow<List<UserWithStatus>>(emptyList())
    val users = _users.asStateFlow()

    private val _history = MutableStateFlow<List<User>>(emptyList())
    val history = _history.asStateFlow()

    @Volatile private var currentQuery: String = ""

    fun searchFriend(keySearch: String) {
        currentQuery = keySearch
        if (keySearch.isBlank()) {
            _users.value = emptyList()
            getSearchHistory()
            return
        }
        searchUsersUseCase(
            query = keySearch,
            onSuccess = { results ->
            if (keySearch != currentQuery) return@searchUsersUseCase

            val myId = sessionRepository.getAuth()
            val others = results.filter { it.keyAuth != myId }
            if (others.isEmpty()) {
                if (keySearch == currentQuery) _users.value = emptyList()
                return@searchUsersUseCase
            }
            val output = mutableListOf<UserWithStatus>()
            var pending = others.size
            others.forEach { user ->
                getFriendshipStatusUseCase(
                    otherId = user.keyAuth,
                    onSuccess = { status ->
                    synchronized(output) {
                        output.add(UserWithStatus(SocialUiMapper.toUi(user), status))
                        pending--
                        if (pending == 0 && keySearch == currentQuery) {
                            _users.value = output.sortedBy { it.user.name }
                        }
                    }
                },
                )
            }
        },
        )
    }

    fun sendFriendRequest(target: User) = viewModelScope.launch {
        sendFriendRequestUseCase(
            target = SocialUiMapper.toDomain(target),
            onSuccess = {
                showMessage("Đã gửi lời mời kết bạn")
                updateUserStatus(target.keyAuth.orEmpty(), "pending_sent")
            },
            onFailure = { showError(it) },
        )
    }

    fun cancelFriendRequest(target: User) = viewModelScope.launch {
        cancelFriendRequestUseCase(
            toId = target.keyAuth.orEmpty(),
            onSuccess = {
                showMessage("Đã huỷ lời mời kết bạn")
                updateUserStatus(target.keyAuth.orEmpty(), "none")
            },
            onFailure = { showError(it) },
        )
    }

    private fun updateUserStatus(keyAuth: String, newStatus: String) {
        _users.value = _users.value.map { item ->
            if (item.user.keyAuth == keyAuth) item.copy(status = newStatus) else item
        }
    }

    fun getSearchHistory() = viewModelScope.launch {
        getSearchHistoryUseCase(onSuccess = { _history.value = SocialUiMapper.toUiUsers(it) })
    }

    fun saveSearchHistory(user: User) = viewModelScope.launch {
        saveSearchHistoryUseCase(SocialUiMapper.toDomain(user))
    }
}

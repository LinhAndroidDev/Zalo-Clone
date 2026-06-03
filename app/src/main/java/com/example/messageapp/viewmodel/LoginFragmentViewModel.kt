package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.data.legacy.PresenceManager
import com.example.messageapp.domain.repository.AuthRepository
import com.example.messageapp.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginFragmentViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val presenceManager: PresenceManager,
) : BaseViewModel() {

    private val _loginSuccessful = MutableStateFlow(false)
    var loginSuccessful: StateFlow<Boolean> = _loginSuccessful

    fun handlerActionLogin(email: String, password: String) = viewModelScope.launch {
        showLoading(true)
        authRepository.checkLogin(
            email = email,
            password = password,
            onSuccess = { users ->
                showLoading(false)
                val user = users.firstOrNull()
                if (user != null && user.keyAuth.isNotBlank()) {
                    sessionRepository.saveAuth(user.keyAuth)
                    sessionRepository.saveNameUser(user.name)
                    sessionRepository.saveStatusLoggedIn(true)
                    presenceManager.connect(user.keyAuth)
                    _loginSuccessful.value = true
                } else {
                    showError("Đăng nhập thất bại")
                }
            },
            onFailure = { error ->
                showLoading(false)
                showError(error)
            },
        )
    }
}

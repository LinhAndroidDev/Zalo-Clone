package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.social.GetCurrentUserUseCase
import com.example.messageapp.mapper.SocialUiMapper
import com.example.messageapp.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QRCodeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
) : BaseViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    fun loadCurrentUser() = viewModelScope.launch {
        getCurrentUserUseCase(onSuccess = { user ->
            _currentUser.value = SocialUiMapper.toUi(user).copy(keyAuth = sessionRepository.getAuth())
        })
    }
}

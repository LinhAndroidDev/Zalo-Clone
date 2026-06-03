package com.example.messageapp.viewmodel

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.social.GetUserInfoUseCase
import com.example.messageapp.domain.usecase.social.UploadProfileImageUseCase
import com.example.messageapp.mapper.SocialUiMapper
import com.example.messageapp.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonalActivityViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val uploadProfileImageUseCase: UploadProfileImageUseCase,
) : BaseViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()
    private val _isInfoUser = MutableStateFlow(true)
    val isInfoUser = _isInfoUser.asStateFlow()

    fun getInfoUser(arg: String?) = viewModelScope.launch {
        _isInfoUser.value = arg == null
        if (isInfoUser.value) {
            getUserInfoUseCase(
                userId = sessionRepository.getAuth(),
                onSuccess = { user -> _user.value = SocialUiMapper.toUi(user) },
            )
        } else {
            getUserInfoUseCase(
                userId = arg.orEmpty(),
                onSuccess = { user -> _user.value = SocialUiMapper.toUi(user) },
            )
        }
    }

    fun uploadPhoto(uri: Uri, isAvatar: Boolean) = viewModelScope.launch {
        uploadProfileImageUseCase(
            uriString = uri.toString(),
            isAvatar = isAvatar,
            onSuccess = {},
            onFailure = { showError(it) },
        )
    }
}

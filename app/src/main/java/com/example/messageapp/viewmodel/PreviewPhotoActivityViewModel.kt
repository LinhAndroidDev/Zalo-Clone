package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.usecase.social.GetUserInfoUseCase
import com.example.messageapp.mapper.SocialUiMapper
import com.example.messageapp.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreviewPhotoActivityViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
) : BaseViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    fun getInfo(keyId: String) = viewModelScope.launch {
        getUserInfoUseCase(
            userId = keyId,
            onSuccess = { user -> _user.value = SocialUiMapper.toUi(user) },
        )
    }
}

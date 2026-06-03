package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.usecase.social.RegisterUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BottomSheetRegisterViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase,
) : BaseViewModel() {

    private val _registerSuccessful = MutableStateFlow(false)
    val registerSuccessful: StateFlow<Boolean> = _registerSuccessful

    fun handlerActionRegister(name: String, email: String, password: String) =
        viewModelScope.launch {
            showLoading(true)
            registerUserUseCase(
                name = name,
                email = email,
                password = password,
                onSuccess = {
                    showLoading(false)
                    _registerSuccessful.value = true
                    showMessage("Đăng ký thành công")
                },
                onFailure = { error ->
                    showLoading(false)
                    showError(error)
                },
            )
        }
}

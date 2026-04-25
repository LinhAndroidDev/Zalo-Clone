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

@HiltViewModel
class QRCodeViewModel @Inject constructor() : BaseViewModel() {

    @Inject
    lateinit var shared: SharePreferenceRepository

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    fun loadCurrentUser() = viewModelScope.launch {
        FireBaseInstance.getInfoUser(shared.getAuth()) { user ->
            _currentUser.value = user.copy(keyAuth = shared.getAuth())
        }
    }
}

package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.User
import com.example.messageapp.mapper.SocialUiMapper
import com.example.messageapp.utils.FireBaseInstance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ScanResult {
    object Idle : ScanResult()
    object Loading : ScanResult()
    data class UserFound(val user: User) : ScanResult()
    data class Error(val message: String) : ScanResult()
}

@HiltViewModel
class ScanQRFragmentViewModel @Inject constructor() : BaseViewModel() {

    private val _scanResult = MutableStateFlow<ScanResult>(ScanResult.Idle)
    val scanResult = _scanResult.asStateFlow()

    fun verifyScannedId(scannedText: String) = viewModelScope.launch {
        if (_scanResult.value is ScanResult.Loading) return@launch
        _scanResult.value = ScanResult.Loading

        FireBaseInstance.getUserById(
            userId = scannedText.trim(),
            success = { user -> _scanResult.value = ScanResult.UserFound(SocialUiMapper.toUi(user)) },
            failure = { error -> _scanResult.value = ScanResult.Error(error) }
        )
    }

    fun resetState() {
        _scanResult.value = ScanResult.Idle
    }
}

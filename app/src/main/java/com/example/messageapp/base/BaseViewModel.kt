package com.example.messageapp.base

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class BaseViewModel : ViewModel() {
    private var _loadingState = MutableStateFlow(false)
    var loadingState: StateFlow<Boolean> = _loadingState
    private var _messageState = MutableStateFlow("")
    var messageState: StateFlow<String> = _messageState
    private var _errorState = MutableStateFlow("")
    var errorState: StateFlow<String> = _errorState
    val db by lazy { Firebase.firestore }


    fun showLoading(isLoading: Boolean) {
        _loadingState.value = isLoading
    }

    fun showMessage(message: String) {
        _messageState.value = message
    }

    fun showError(error: String) {
        _errorState.value = error
    }
}
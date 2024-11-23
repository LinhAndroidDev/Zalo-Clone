package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.Message
import com.example.messageapp.model.User
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreviewPhotoActivityViewModel @Inject constructor() : BaseViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository
    private val _user: MutableStateFlow<User?> = MutableStateFlow(null)
    val user = _user.asStateFlow()

    fun getInfo(message: Message, keyId: String) = viewModelScope.launch {
        FireBaseInstance.getInfoUser(keyId) { data ->
            _user.value = data
        }
    }
}
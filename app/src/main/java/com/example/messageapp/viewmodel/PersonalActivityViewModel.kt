package com.example.messageapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.User
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PersonalActivityViewModel @Inject constructor() : BaseViewModel() {

    @Inject
    lateinit var shared: SharePreferenceRepository

    private val _user: MutableStateFlow<User?> = MutableStateFlow(null)
    val user = _user.asStateFlow()

    fun getInfoUser() = viewModelScope.launch {
        FireBaseInstance.getInfoUser(
            shared.getAuth(),
            success = { user ->
                _user.value = user
            }
        )
    }

    fun uploadPhoto(context: Context, uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        FireBaseInstance.uploadImage(context ,uriPhoto = uri) {
            updateAvatarUser(it, shared.getAuth())
        }
    }

    private fun updateAvatarUser(avatar: String, keyAuth: String) =
        viewModelScope.launch(Dispatchers.IO) {
            FireBaseInstance.updateAvatarUser(avatar = avatar, keyAuth = keyAuth)
        }
}
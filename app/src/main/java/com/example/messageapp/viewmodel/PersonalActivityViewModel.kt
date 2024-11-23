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
import javax.inject.Inject

@HiltViewModel
class PersonalActivityViewModel @Inject constructor() : BaseViewModel() {

    @Inject
    lateinit var shared: SharePreferenceRepository

    private val _user: MutableStateFlow<User?> = MutableStateFlow(null)
    val user = _user.asStateFlow()

    /** This function is used to get information of user */
    fun getInfoUser() = viewModelScope.launch {
        FireBaseInstance.getInfoUser(
            shared.getAuth(),
            success = { user ->
                _user.value = user
            }
        )
    }

    /** This function is used to upload photo to firebase*/
    fun uploadPhoto(context: Context, uri: Uri, isAvatar: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        FireBaseInstance.uploadImage(context ,uriPhoto = uri) {
            if (isAvatar) {
                FireBaseInstance.updateAvatarUser(avatar = it, userId = shared.getAuth())
            } else {
                FireBaseInstance.updateImageCover(imageCover = it, userId = shared.getAuth())
            }
        }
    }
}
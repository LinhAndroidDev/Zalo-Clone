package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.Friend
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import com.google.firebase.firestore.QuerySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : BaseViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository

    private val _friends: MutableStateFlow<MutableList<Friend>?> = MutableStateFlow(null)
    val friends = _friends.asStateFlow()

    fun getSuggestFriend() = viewModelScope.launch {
        showLoading(true)
        FireBaseInstance.getUsers(
            success = { result ->
                showLoading(false)
                handlerGetSuggestFriend(result) {
                    _friends.value = it
                }
            },
            failure = { error ->
                showLoading(false)
                showError(error)
            }
        )
    }

    private fun handlerGetSuggestFriend(result: QuerySnapshot, success: (MutableList<Friend>) -> Unit) {
        val friendData = mutableListOf<Friend>()
        for(document in result.documents) {
            if(document.id != shared.getAuth()) {
                document.data?.let { data ->
                    friendData.add(Friend(data["name"].toString(), data["avatar"].toString()))
                }
            }
        }
        success.invoke(friendData)
    }
}
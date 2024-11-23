package com.example.messageapp.viewmodel

import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.User
import com.example.messageapp.utils.FireBaseInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchFragmentViewModel : BaseViewModel() {
    private val _users: MutableStateFlow<ArrayList<User>> = MutableStateFlow(arrayListOf())
    val users = _users.asStateFlow()

    fun searchFriend(keySearch: String) {
        FireBaseInstance.searchFriend(queryText = keySearch) {
            _users.value = it
        }
    }
}
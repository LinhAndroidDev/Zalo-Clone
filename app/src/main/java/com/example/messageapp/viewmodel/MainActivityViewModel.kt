package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor() : BaseViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository

    private var _numberMsgUnSeen: MutableStateFlow<Int> = MutableStateFlow(0)
    val numberMsgUnSeen = _numberMsgUnSeen.asStateFlow()

    /**
     * This function used to get number of message unread
     */
    fun getNumberUnSeen() = viewModelScope.launch {
        FireBaseInstance.getNumberUnreadMessages(shared.getAuth()) {
            _numberMsgUnSeen.value = it
        }
    }
}
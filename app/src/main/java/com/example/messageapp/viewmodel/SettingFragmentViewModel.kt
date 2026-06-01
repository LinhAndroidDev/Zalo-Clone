package com.example.messageapp.viewmodel

import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.utils.PresenceManager
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingFragmentViewModel @Inject constructor() : BaseViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository

    @Inject
    lateinit var presenceManager: PresenceManager

    fun actionLogout() {
        val userId = shared.getAuth()
        if (userId.isNotBlank()) {
            presenceManager.disconnect(userId)
        }
        shared.saveStatusLoggedIn(false)
        shared.saveNameUser("")
        shared.saveAuth("")
    }
}
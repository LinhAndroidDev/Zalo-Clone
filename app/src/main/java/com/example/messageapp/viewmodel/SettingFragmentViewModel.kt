package com.example.messageapp.viewmodel

import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.data.legacy.PresenceManager
import com.example.messageapp.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingFragmentViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val presenceManager: PresenceManager,
) : BaseViewModel() {

    fun actionLogout() {
        val userId = sessionRepository.getAuth()
        if (userId.isNotBlank()) {
            presenceManager.disconnect(userId)
        }
        sessionRepository.saveStatusLoggedIn(false)
        sessionRepository.saveNameUser("")
        sessionRepository.saveAuth("")
    }
}

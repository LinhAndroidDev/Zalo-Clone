package com.example.messageapp.viewmodel

import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashFragmentViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : BaseViewModel() {
    val isLogin: Boolean
        get() = sessionRepository.getStatusLoggedIn()
}

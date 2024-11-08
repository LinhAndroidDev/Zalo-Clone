package com.example.messageapp.viewmodel

import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingFragmentViewModel @Inject constructor() : BaseViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository

    fun actionLogout() {
        shared.saveStatusLoggedIn(false)
        shared.saveNameUser("")
        shared.saveAuth("")
    }
}
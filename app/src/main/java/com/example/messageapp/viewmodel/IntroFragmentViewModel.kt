package com.example.messageapp.viewmodel

import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IntroFragmentViewModel @Inject constructor() : BaseViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository
}
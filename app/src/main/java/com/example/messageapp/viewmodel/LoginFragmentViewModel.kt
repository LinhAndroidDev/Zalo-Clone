package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginFragmentViewModel @Inject constructor() : BaseViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository

    private val _loginSuccessful = MutableStateFlow(false)
    var loginSuccessful: StateFlow<Boolean> = _loginSuccessful

    /**
     * This function handle action login
     * @param email is email which user registered
     * @param password is password which user registered with email
     */
    fun handlerActionLogin(email: String, password: String) = viewModelScope.launch {
        showLoading(true)
        FireBaseInstance.checkLogin(
            email = email,
            password = password,
            success = { result ->
                showLoading(false)
                handlerLoginSuccess(result)
            },
            failure = { error ->
                showLoading(false)
                showError(error)
            }
        )
    }

    /**
     * This function handler when login success
     * @param result get data user and save to share preference
     * Then send action _loginSuccessful to Fragment
     */
    private fun handlerLoginSuccess(result: QuerySnapshot) {
        if (result.isEmpty) {
            showError("Dont Exist Account")
        } else {
            result.forEach { document ->
                val data = document.data as Map<*, *>
                shared.saveAuth(document.id)
                shared.saveNameUser(data["name"].toString())
                shared.saveStatusLoggedIn(true)
                _loginSuccessful.value = true
            }
        }
    }
}
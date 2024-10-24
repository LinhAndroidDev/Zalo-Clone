package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import com.google.firebase.firestore.QuerySnapshot
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
        FireBaseInstance.getUsers(
            success = { result ->
                showLoading(false)
                handlerLoginSuccess(email, password, result)
            },
            failure = { error ->
                showLoading(false)
                showError(error)
            }
        )
    }

    /**
     * This function handler when login success
     * @param result check exist account use email and password
     * Then send action _loginSuccessful to Fragment
     */
    private fun handlerLoginSuccess(email: String, password: String, result: QuerySnapshot) {
        var isExistAccount = false
        for (document in result.documents) {
            val data = document.data as Map<*, *>
            if (data["email"] == email && data["password"] == password) {
                isExistAccount = true
                shared.saveAuth(document.id)
                _loginSuccessful.value = true
                break
            }
        }
        if (!isExistAccount) {
            showError("Dont Exist Account")
        }

    }
}
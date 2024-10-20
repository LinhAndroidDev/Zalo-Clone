package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginFragmentViewModel : BaseViewModel() {
    private val _loginSuccessful = MutableStateFlow(false)
    var loginSuccessful: StateFlow<Boolean> = _loginSuccessful

    /**
     * This function handle action login
     * @param email is email which user registered
     * @param password is password which user registered with email
     */
    fun handlerActionLogin(email: String, password: String) = viewModelScope.launch {
        showLoading(true)
        val docRef = db.collection("users").get()

        docRef.addOnSuccessListener { result ->
            showLoading(false)

            var isExistAccount = false
            for (document in result) {
                val data = document.data as Map<String, String?>
                if (data["email"] == email && data["password"] == password) {
                    isExistAccount = true
                    _loginSuccessful.value = true
                    break
                }
            }

            if (!isExistAccount) {
                showError("Dont Exist Account")
            }
        }.addOnFailureListener { e ->
            showLoading(false)
            showError(e.message.toString())
        }
    }
}
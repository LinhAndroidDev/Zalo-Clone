package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.utils.FireBaseInstance
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BottomSheetRegisterViewModel : BaseViewModel() {
    private val _registerSuccessful = MutableStateFlow(false)
    var registerSuccessful: StateFlow<Boolean> = _registerSuccessful

    fun handlerActionRegister(name: String, email: String, password: String) =
        viewModelScope.launch {
            val user = hashMapOf(
                "name" to name,
                "email" to email,
                "password" to password,
                "avatar" to ""
            )

            checkExistEmail(email) { isExist ->
                if (!isExist) {
                    showLoading(true)
                    FireBaseInstance.addUser(user,
                        success = { message ->
                            showLoading(false)
                            _registerSuccessful.value = true
                            showMessage(message)
                        },
                        failure = { error ->
                            showLoading(false)
                            showError(error)
                        })
                } else {
                    showError("Email already exists")
                }
            }
        }

    private fun checkExistEmail(email: String, callback: (Boolean) -> Unit) {
        showLoading(true)
        FireBaseInstance.getUsers(
            success = { result ->
                showLoading(false)
                handleExistEmail(email, result, callback)
            },
            failure = { error ->
                showLoading(false)
                showError(error)
                callback.invoke(false)
            }
        )
    }

    private fun handleExistEmail(
        email: String,
        result: QuerySnapshot,
        callback: (Boolean) -> Unit
    ) {
        var isExistEmail = false
        for (document in result) {
            val data = document.data as Map<*, *>
            if (data["email"] == email) {
                isExistEmail = true
                break
            }
        }
        callback.invoke(isExistEmail)
    }
}
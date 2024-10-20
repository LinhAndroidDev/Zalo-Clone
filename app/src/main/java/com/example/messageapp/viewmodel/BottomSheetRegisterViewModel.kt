package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
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
                    db.collection("users")
                        .add(user)
                        .addOnSuccessListener {
                            showLoading(false)
                            _registerSuccessful.value = true
                            showMessage("Create Successful")
                        }
                        .addOnFailureListener { e ->
                            showLoading(false)
                            showError(e.message.toString())
                        }
                } else {
                    showError("Email already exists")
                }
            }
        }

    private fun checkExistEmail(email: String, callback: (Boolean) -> Unit) {
        showLoading(true)
        val docRef = db.collection("users").get()

        docRef.addOnSuccessListener { result ->
            showLoading(false)
            var isExistEmail = false
            for (document in result) {
                val data = document.data as Map<String, String?>
                if (data["email"] == email) {
                    isExistEmail = true
                    break
                }
            }
            callback.invoke(isExistEmail)
        }.addOnFailureListener { e ->
            showLoading(false)
            showError(e.message.toString())
            callback.invoke(false)
        }
    }
}
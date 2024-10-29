package com.example.messageapp.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.messageapp.MyApplication
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.User
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : BaseViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository

    private val _friends: MutableStateFlow<MutableList<User>?> = MutableStateFlow(null)
    val friends = _friends.asStateFlow()
    private val _conversation: MutableStateFlow<ArrayList<Conversation>?> = MutableStateFlow(null)
    val conversation = _conversation.asStateFlow()

    fun getSuggestFriend() = viewModelScope.launch {
        showLoading(true)
        FireBaseInstance.getUsers(
            success = { result ->
                showLoading(false)
                handlerGetSuggestFriend(result) {
                    _friends.value = it
                }
            },
            failure = { error ->
                showLoading(false)
                showError(error)
            }
        )
    }

    private fun handlerGetSuggestFriend(result: QuerySnapshot, success: (MutableList<User>) -> Unit) {
        val friendData = mutableListOf<User>()
        for(document in result.documents) {
            if(document.id != shared.getAuth()) {
                document.data?.let { data ->
                    friendData.add(
                        User(
                            data["name"].toString(),
                            data["email"].toString(),
                            data["avatar"].toString(),
                            document.id
                        )
                    )
                }
            }
        }
        success.invoke(friendData)
    }

    fun getListConversation() = viewModelScope.launch {
        FireBaseInstance.getListConversation(shared.getAuth(),
            success = { result ->
                val conversationData = arrayListOf<Conversation>()
                result?.forEach { document ->
                    val conversation = document.toObject(Conversation::class.java)
                    if (conversation.sender == shared.getAuth()) {
                        conversationData.add(conversation)
                    }
                }
                _conversation.value = conversationData
            },
            failure = { error ->
                showError(error)
            })
    }

    fun generateToken() {
        val firebaseInstance = FirebaseInstallations.getInstance()
        firebaseInstance.id.addOnSuccessListener {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { gettocken ->
                val hasHamp = hashMapOf<String, String>("token" to gettocken)
                FireBaseInstance.saveTokenMessage(shared.getAuth(), hasHamp)
            }
        }.addOnFailureListener {
            Log.e("GenerateToken", "Fail")
        }
    }
}
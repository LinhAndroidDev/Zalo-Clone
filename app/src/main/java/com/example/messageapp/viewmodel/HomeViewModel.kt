package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.User
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
import com.google.firebase.firestore.QuerySnapshot
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
    private var _numberMsgUnSeen: MutableStateFlow<Int> = MutableStateFlow(0)
    val numberMsgUnSeen = _numberMsgUnSeen.asStateFlow()

    // This function used to get list suggest friend
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

    /**
     * This function used to handle get suggest friend
     * @param result data from FireStore
     * @param success callback function
     */
    private fun handlerGetSuggestFriend(result: QuerySnapshot, success: (MutableList<User>) -> Unit) {
        val friendData = mutableListOf<User>()
        for(document in result.documents) {
            if(document.id != shared.getAuth()) {
                document.data?.let { data ->
                    friendData.add(
                        User(
                            name = data["name"].toString(),
                            email = data["email"].toString(),
                            avatar = data["avatar"].toString(),
                            imageCover = data["imageCover"].toString(),
                            keyAuth = document.id
                        )
                    )
                }
            }
        }
        success.invoke(friendData)
    }

    // This function used to get list conversation
    fun getListConversation() = viewModelScope.launch {
        FireBaseInstance.getListConversation(shared.getAuth(),
            success = { result ->
                val conversationData = arrayListOf<Conversation>()
                result?.forEach { document ->
                    val conversation = document.toObject(Conversation::class.java)
                    conversationData.add(conversation)
                }
                _conversation.value = conversationData
            },
            failure = { error ->
                showError(error)
            })
    }

   // This function used to generate token for message
    fun generateToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { gettocken ->
            val hasHamp = hashMapOf<String, String>("token" to gettocken)
            FireBaseInstance.saveTokenMessage(shared.getAuth(), hasHamp)
        }
    }

    // This function used to get number of message unread
    fun getNumberUnSeen() = viewModelScope.launch {
        FireBaseInstance.getNumberUnreadMessages(shared.getAuth()) {
            _numberMsgUnSeen.value = it
        }
    }
}
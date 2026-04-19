package com.example.messageapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.model.Conversation
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.SharePreferenceRepository
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

    private val _conversation: MutableStateFlow<ArrayList<Conversation>?> = MutableStateFlow(null)
    val conversation = _conversation.asStateFlow()
    private var _numberMsgUnSeen: MutableStateFlow<Int> = MutableStateFlow(0)
    val numberMsgUnSeen = _numberMsgUnSeen.asStateFlow()

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

    fun generateToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val hashMap = hashMapOf<String, String>("token" to token)
            FireBaseInstance.saveTokenMessage(shared.getAuth(), hashMap)
        }
    }

    fun getNumberUnSeen() = viewModelScope.launch {
        FireBaseInstance.getNumberUnreadMessages(shared.getAuth()) {
            _numberMsgUnSeen.value = it
        }
    }
}

package com.example.messageapp.utils

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase

object FirebaseAnalyticsInstance {
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    private const val SCREEN_HOME = "screen_home"
    private const val SCREEN_CHAT = "screen_chat"
    private const val SCREEN_PERSONAL = "screen_personal"
    private const val SCREEN_DIARY = "screen_diary"
    private const val SCREEN_DISCOVER = "screen_discover"
    private const val SCREEN_SETTING = "screen_setting"
    private const val SCREEN_FRIEND_REQUEST = "screen_friend_request"
    private const val SCREEN_PHONE_BOOK = "screen_phone_book"
    private const val SCREEN_SEARCH = "screen_search"

    // Message event parameters
    private const val EVENT_SEND_MESSAGE = "send_message"
    private const val PARAM_MESSAGE_TYPE = "message_type"
    private const val PARAM_MESSAGE_LENGTH = "message_length"
    private const val PARAM_RECEIVER_ID = "receiver_id"

    // Screen events
    private const val EVENT_SCREEN_VIEW = "screen_view"
    private const val PARAM_SCREEN_NAME = "screen_name"
    private const val PARAM_SCREEN_TYPE = "screen_type"

    private fun logScreenView(screenName: String, screenType: String = "main") {
        firebaseAnalytics.logEvent(EVENT_SCREEN_VIEW) {
            param(PARAM_SCREEN_NAME, screenName)
            param(PARAM_SCREEN_TYPE, screenType)
        }
    }

    // Screen logging methods
    fun logHomeScreen() {
        logScreenView(SCREEN_HOME)
    }

    fun logChatScreen() {
        logScreenView(SCREEN_CHAT)
    }

    fun logPersonalScreen() {
        logScreenView(SCREEN_PERSONAL)
    }

    fun logDiaryScreen() {
        logScreenView(SCREEN_DIARY)
    }

    fun logDiscoverScreen() {
        logScreenView(SCREEN_DISCOVER)
    }

    fun logSettingScreen() {
        logScreenView(SCREEN_SETTING)
    }

    fun logFriendRequestScreen() {
        logScreenView(SCREEN_FRIEND_REQUEST, "sub")
    }

    fun logPhoneBookScreen() {
        logScreenView(SCREEN_PHONE_BOOK, "sub")
    }

    fun logSearchScreen() {
        logScreenView(SCREEN_SEARCH, "sub")
    }

    // Message logging method
    fun logSendMessage(messageType: String, messageLength: Int, receiverId: String) {
        firebaseAnalytics.logEvent(EVENT_SEND_MESSAGE) {
            param(PARAM_MESSAGE_TYPE, messageType)
            param(PARAM_MESSAGE_LENGTH, messageLength.toLong())
            param(PARAM_RECEIVER_ID, receiverId)
        }
    }
}
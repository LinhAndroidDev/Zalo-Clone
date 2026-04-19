package com.example.messageapp.utils

import com.example.messageapp.fragment.Language

interface SharePreferenceRepository {
    companion object {
        const val LANGUAGE_SELECTED = "LANGUAGE_SELECTED"
        const val KEY_AUTH = "KEY_AUTH"
        const val NAME_USER = "NAME_USER"
        const val CHANNEL_ID = "CHANNEL_ID"
        const val STATUS_LOGGED_IN = "STATUS_LOGGED_IN"
        const val LAST_SEEN_FRIEND_REQUEST_AT = "LAST_SEEN_FRIEND_REQUEST_AT"
    }

    fun saveLanguageSelected(language: Language)

    fun getLanguageSelected(): Language

    fun saveAuth(auth: String)

    fun getAuth(): String

    fun saveNameUser(name: String)

    fun getNameUser(): String

    fun saveChannelId(channelId: Int)

    fun getChannelId(): Int

    fun saveStatusLoggedIn(status: Boolean)

    fun getStatusLoggedIn(): Boolean

    fun saveLastSeenFriendRequestAt(time: Long)

    fun getLastSeenFriendRequestAt(): Long
}
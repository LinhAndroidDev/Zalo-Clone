package com.example.messageapp.domain.repository

import com.example.messageapp.domain.model.AppLanguage

interface SessionRepository {
    fun saveLanguageSelected(language: AppLanguage)
    fun getLanguageSelected(): AppLanguage
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
    fun saveLastSeenDiaryNotificationAt(time: Long)
    fun getLastSeenDiaryNotificationAt(): Long
}

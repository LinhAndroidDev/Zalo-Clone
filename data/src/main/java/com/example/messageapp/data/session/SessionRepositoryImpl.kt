package com.example.messageapp.data.session

import android.content.Context
import com.example.messageapp.data.local.PreferenceUtil
import com.example.messageapp.domain.model.AppLanguage
import com.example.messageapp.domain.repository.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    @ApplicationContext appContext: Context,
) : SessionRepository, PreferenceUtil(appContext) {

    private val prefs by lazy { defaultPref() }

    override fun saveLanguageSelected(language: AppLanguage) {
        prefs[LANGUAGE_SELECTED] = language.ordinal
    }

    override fun getLanguageSelected(): AppLanguage {
        val language = prefs[LANGUAGE_SELECTED, 0]
        return AppLanguage.fromOrdinal(language ?: 0)
    }

    override fun saveAuth(auth: String) {
        prefs[KEY_AUTH] = auth
    }

    override fun getAuth(): String = prefs[KEY_AUTH] ?: ""

    override fun saveNameUser(name: String) {
        prefs[NAME_USER] = name
    }

    override fun getNameUser(): String = prefs[NAME_USER] ?: ""

    override fun saveChannelId(channelId: Int) {
        prefs[CHANNEL_ID] = channelId
    }

    override fun getChannelId(): Int = prefs[CHANNEL_ID] ?: 0

    override fun saveStatusLoggedIn(status: Boolean) {
        prefs[STATUS_LOGGED_IN] = status
    }

    override fun getStatusLoggedIn(): Boolean = prefs[STATUS_LOGGED_IN] ?: false

    override fun saveLastSeenFriendRequestAt(time: Long) {
        prefs[LAST_SEEN_FRIEND_REQUEST_AT] = time
    }

    override fun getLastSeenFriendRequestAt(): Long = prefs[LAST_SEEN_FRIEND_REQUEST_AT] ?: 0L

    override fun saveLastSeenDiaryNotificationAt(time: Long) {
        prefs[LAST_SEEN_DIARY_NOTIFICATION_AT] = time
    }

    override fun getLastSeenDiaryNotificationAt(): Long = prefs[LAST_SEEN_DIARY_NOTIFICATION_AT] ?: 0L

    companion object {
        const val LANGUAGE_SELECTED = "LANGUAGE_SELECTED"
        const val KEY_AUTH = "KEY_AUTH"
        const val NAME_USER = "NAME_USER"
        const val CHANNEL_ID = "CHANNEL_ID"
        const val STATUS_LOGGED_IN = "STATUS_LOGGED_IN"
        const val LAST_SEEN_FRIEND_REQUEST_AT = "LAST_SEEN_FRIEND_REQUEST_AT"
        const val LAST_SEEN_DIARY_NOTIFICATION_AT = "LAST_SEEN_DIARY_NOTIFICATION_AT"
    }
}

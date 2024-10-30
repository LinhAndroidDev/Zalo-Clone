package com.example.messageapp.utils

import android.content.Context
import com.example.messageapp.fragment.Language

class SharePreferenceRepositoryImpl(private val ctx: Context) : SharePreferenceRepository,
    PreferenceUtil(ctx) {

    private val prefs by lazy { defaultPref() }
    override fun saveLanguageSelected(language: Language) {
        prefs[SharePreferenceRepository.LANGUAGE_SELECTED] = language.ordinal
    }

    override fun getLanguageSelected(): Language {
        val language = prefs[SharePreferenceRepository.LANGUAGE_SELECTED, 0]
        return Language.of(language ?: 0)
    }

    override fun saveAuth(auth: String) {
        prefs[SharePreferenceRepository.KEY_AUTH] = auth
    }

    override fun getAuth(): String {
        return prefs[SharePreferenceRepository.KEY_AUTH] ?: ""
    }

    override fun saveNameUser(name: String) {
        prefs[SharePreferenceRepository.NAME_USER] = name
    }

    override fun getNameUser(): String {
        return prefs[SharePreferenceRepository.NAME_USER] ?: ""
    }

    override fun saveAvatarUser(avatar: String) {
        prefs[SharePreferenceRepository.AVATAR_USER] = avatar
    }

    override fun getAvatarUser(): String {
        return prefs[SharePreferenceRepository.AVATAR_USER] ?: ""
    }

    override fun saveChannelId(channelId: Int) {
        prefs[SharePreferenceRepository.CHANNEL_ID] = channelId
    }

    override fun getChannelId(): Int {
        return prefs[SharePreferenceRepository.CHANNEL_ID] ?: 0
    }
}
package com.example.messageapp.utils

import com.example.messageapp.fragment.Language

interface SharePreferenceRepository {
    companion object {
        const val LANGUAGE_SELECTED = "LANGUAGE_SELECTED"
        const val KEY_AUTH = "KEY_AUTH"
        const val NAME_USER = "NAME_USER"
        const val AVATAR_USER = "AVATAR_USER"
        const val CHANNEL_ID = "CHANNEL_ID"
    }

    fun saveLanguageSelected(language: Language)

    fun getLanguageSelected(): Language

    fun saveAuth(auth: String)

    fun getAuth(): String

    fun saveNameUser(name: String)

    fun getNameUser(): String

    fun saveAvatarUser(avatar: String)

    fun getAvatarUser(): String

    fun saveChannelId(channelId: Int)

    fun getChannelId(): Int
}
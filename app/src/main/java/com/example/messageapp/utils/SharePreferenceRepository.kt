package com.example.messageapp.utils

import com.example.messageapp.fragment.Language

interface SharePreferenceRepository {
    companion object {
        const val LANGUAGE_SELECTED = "LANGUAGE_SELECTED"
        const val KEY_AUTH = "KEY_AUTH"
    }

    fun saveLanguageSelected(language: Language)

    fun getLanguageSelected(): Language

    fun saveAuth(auth: String)

    fun getAuth(): String
}
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
}
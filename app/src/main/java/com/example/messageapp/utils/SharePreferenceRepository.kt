package com.example.messageapp.utils

import com.example.messageapp.fragment.Language

interface SharePreferenceRepository {
    companion object {
        const val LANGUAGE_SELECTED = "LANGUAGE_SELECTED"
    }

    fun saveLanguageSelected(language: Language)

    fun getLanguageSelected(): Language
}
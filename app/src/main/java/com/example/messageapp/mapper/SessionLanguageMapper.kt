package com.example.messageapp.mapper

import com.example.messageapp.domain.model.AppLanguage
import com.example.messageapp.fragment.Language

object SessionLanguageMapper {
    fun toAppLanguage(language: Language): AppLanguage = when (language) {
        Language.VIETNAMESE -> AppLanguage.VIETNAMESE
        Language.ENGLISH -> AppLanguage.ENGLISH
    }

    fun toUiLanguage(language: AppLanguage): Language = when (language) {
        AppLanguage.VIETNAMESE -> Language.VIETNAMESE
        AppLanguage.ENGLISH -> Language.ENGLISH
    }
}

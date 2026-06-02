package com.example.messageapp.domain.model

enum class AppLanguage {
    VIETNAMESE,
    ENGLISH,
    ;

    companion object {
        fun fromOrdinal(value: Int): AppLanguage =
            entries.getOrElse(value) { VIETNAMESE }
    }
}

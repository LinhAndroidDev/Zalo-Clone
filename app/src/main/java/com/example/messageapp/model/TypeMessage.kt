package com.example.messageapp.model

enum class TypeMessage(val rawValue: Int) {
    MESSAGE(0),
    PHOTOS(1),
    SINGLE_PHOTO(2);

    companion object {
        fun of(value: Int): TypeMessage {
            return TypeMessage.entries.firstOrNull { it.rawValue == value }
                ?: throw IllegalArgumentException("Unknown Data")
        }
    }
}
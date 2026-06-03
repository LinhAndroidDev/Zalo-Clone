package com.example.messageapp.data.firestore

enum class TypeMessage(val rawValue: Int) {
    MESSAGE(0),
    PHOTOS(1),
    SINGLE_PHOTO(2),
    AUDIO(3),
    SYSTEM(4);

    companion object {
        fun of(value: Int): TypeMessage {
            return entries.firstOrNull { it.rawValue == value } ?: MESSAGE
        }
    }
}
package com.example.messageapp.data.firestore

enum class Sticker(val value: String, val index: Int) {
    HELLO("hello", 0),
    LOVE("love", 1),
    CONGRATULATION("congatulation", 2),
    ANGRY("angry", 3),
    SAD("sad", 4),
    SORRY("sorry", 5),
    ;

    companion object {
        fun of(value: Int): Sticker = entries.find { it.index == value } ?: HELLO
    }
}

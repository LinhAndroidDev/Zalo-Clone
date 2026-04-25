package com.example.messageapp.model

import com.example.messageapp.R

enum class Sticker(val value: String, val index: Int, val icon: Int = 0) {
    HELLO("hello", 0, R.drawable.ic_hello),
    LOVE("love", 1, R.drawable.ic_love),
    CONGRATULATION("congatulation", 2, R.drawable.ic_congratulation),
    ANGRY("angry", 3, R.drawable.ic_angry),
    SAD("sad", 4, R.drawable.ic_sad),
    SORRY("sorry", 5, R.drawable.ic_sorry);

    companion object {
        fun of(value: Int): Sticker {
            return entries.find { it.index == value } ?: HELLO
        }
    }
}
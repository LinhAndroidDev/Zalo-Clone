package com.example.messageapp.model

/**
 * Create By Nguyen Huu Linh on 16/09/2024
 * This class handle actions click item bottom navigation bar
 * Then send event to activity or fragment to update UI
 */

enum class ActionBottomBar(val rawValue: Int) {
    MESSAGE_VIEW(0),
    PHONE_BOOK_VIEW(1),
    DISCOVER_VIEW(2),
    DIARY_VIEW(3),
    PERSONAL_VIEW(4),
    CHAT_VIEW(5),
    SEARCH_VIEW(6),
    HEADER_TITLE(7);

    companion object {
        fun of(value: Int): ActionBottomBar {
            return entries.firstOrNull { it.rawValue == value }
                ?: throw IllegalArgumentException("Unknown Data")
        }
    }
}
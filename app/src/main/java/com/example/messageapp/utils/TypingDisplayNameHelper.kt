package com.example.messageapp.utils

import android.content.res.Resources
import com.example.messageapp.R

object TypingDisplayNameHelper {

    fun shortDisplayName(fullName: String): String {
        val trimmed = fullName.trim()
        if (trimmed.isBlank()) return ""
        return trimmed.split(Regex("\\s+")).last()
    }

    fun buildTypingLabel(resources: Resources, shortNames: List<String>): String {
        val names = shortNames.filter { it.isNotBlank() }
        return when (names.size) {
            0 -> ""
            1 -> resources.getString(R.string.typing_single, names[0])
            2, 3 -> resources.getString(R.string.typing_multiple, names.joinToString(", "))
            else -> {
                val shown = names.take(2).joinToString(", ")
                val others = names.size - 2
                resources.getString(R.string.typing_multiple_and_others, shown, others)
            }
        }
    }
}

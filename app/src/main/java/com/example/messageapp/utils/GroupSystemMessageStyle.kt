package com.example.messageapp.utils

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import com.example.messageapp.R

object GroupSystemMessageStyle {

    private const val SUFFIX_ADDED = " vào nhóm"
    private const val SUFFIX_REMOVED = " khỏi nhóm"
    private const val SUFFIX_LEFT = " đã rời nhóm"
    private const val MIDDLE_ADDED = " đã thêm "
    private const val MIDDLE_REMOVED = " đã xóa "

    fun styledText(context: Context, text: String): CharSequence {
        if (text.isBlank()) return text
        val names = highlightNames(text)
        val spannable = SpannableString(text)
        val baseColor = ContextCompat.getColor(context, R.color.text_common)
        val nameColor = ContextCompat.getColor(context, R.color.text_common)

        spannable.setSpan(
            ForegroundColorSpan(baseColor),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        names.sortedByDescending { it.length }.forEach { name ->
            if (name.isBlank()) return@forEach
            var start = 0
            while (start < text.length) {
                val index = text.indexOf(name, start, ignoreCase = false)
                if (index < 0) break
                val end = index + name.length
                spannable.setSpan(StyleSpan(Typeface.BOLD), index, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(
                    ForegroundColorSpan(nameColor),
                    index,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                start = end
            }
        }
        return spannable
    }

    private fun highlightNames(text: String): List<String> {
        return when {
            text.contains(MIDDLE_ADDED) && text.endsWith(SUFFIX_ADDED) -> {
                val actor = text.substringBefore(MIDDLE_ADDED).trim()
                val targetsPart = text
                    .substringAfter(MIDDLE_ADDED)
                    .removeSuffix(SUFFIX_ADDED)
                    .trim()
                listOf(actor) + splitDisplayNameList(targetsPart)
            }

            text.contains(MIDDLE_REMOVED) && text.endsWith(SUFFIX_REMOVED) -> {
                val actor = text.substringBefore(MIDDLE_REMOVED).trim()
                val target = text
                    .substringAfter(MIDDLE_REMOVED)
                    .removeSuffix(SUFFIX_REMOVED)
                    .trim()
                listOf(actor, target)
            }

            text.endsWith(SUFFIX_LEFT) -> {
                listOf(text.removeSuffix(SUFFIX_LEFT).trim())
            }

            else -> emptyList()
        }.filter { it.isNotBlank() }.distinct()
    }

    /** "B, C và D" → [B, C, D] */
    private fun splitDisplayNameList(part: String): List<String> {
        if (part.isBlank()) return emptyList()
        return part.split(", ")
            .flatMap { segment ->
                val trimmed = segment.trim()
                if (trimmed.contains(" và ")) {
                    trimmed.split(" và ").map { it.trim() }
                } else {
                    listOf(trimmed)
                }
            }
            .filter { it.isNotBlank() }
    }
}

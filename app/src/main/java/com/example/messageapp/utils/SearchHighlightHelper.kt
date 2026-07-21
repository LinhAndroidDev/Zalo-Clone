package com.example.messageapp.utils

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.example.messageapp.R

object SearchHighlightHelper {

    data class NormalizedText(
        val normalized: String,
        val indexMap: List<Int>,
    )

    fun buildNormalizedText(text: String): NormalizedText {
        val normalizedBuilder = StringBuilder()
        val indexMap = mutableListOf<Int>()
        text.forEachIndexed { index, char ->
            val normalizedChar = removeAccent(char.toString().lowercase())
            normalizedChar.forEach { normalizedSingleChar ->
                normalizedBuilder.append(normalizedSingleChar)
                indexMap.add(index)
            }
        }
        return NormalizedText(normalizedBuilder.toString(), indexMap)
    }

    fun findMatchRange(text: String, query: String): IntRange? {
        val normalizedQuery = removeAccent(query.trim().lowercase())
        if (normalizedQuery.isEmpty()) return null

        val normalizedText = buildNormalizedText(text)
        val start = normalizedText.normalized.indexOf(normalizedQuery)
        if (start < 0) return null

        val endExclusive = start + normalizedQuery.length
        if (endExclusive > normalizedText.indexMap.size) return null

        val originalStart = normalizedText.indexMap[start]
        val originalEndExclusive = normalizedText.indexMap[endExclusive - 1] + 1
        if (originalStart !in text.indices || originalEndExclusive > text.length) return null
        return originalStart until originalEndExclusive
    }

    fun buildHighlightedContent(
        context: Context,
        messageText: String,
        matchStart: Int,
        matchEnd: Int,
        displayTime: String,
    ): SpannableString {
        val suffix = " · $displayTime"
        val fullText = messageText + suffix
        val spannable = SpannableString(fullText)
        if (matchStart in 0..messageText.length && matchEnd in matchStart..messageText.length) {
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.color_primary)),
                matchStart,
                matchEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        return spannable
    }
}

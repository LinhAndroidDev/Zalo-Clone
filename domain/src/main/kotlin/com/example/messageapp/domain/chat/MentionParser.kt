package com.example.messageapp.domain.chat

import com.example.messageapp.domain.model.MessageMention
import java.text.Normalizer
import java.util.regex.Pattern

object MentionParser {

    const val ALL_USER_ID = "__all__"

    data class MentionQuery(
        val startIndex: Int,
        val endIndex: Int,
        val query: String,
    )

    fun removeAccent(input: String): String {
        val withoutDStroke = input
            .replace('đ', 'd')
            .replace('Đ', 'D')
        val normalized = Normalizer.normalize(withoutDStroke, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(normalized).replaceAll("")
    }

    fun detectActiveMentionQuery(text: CharSequence, cursor: Int): MentionQuery? {
        if (cursor < 0 || cursor > text.length) return null
        var index = cursor - 1
        while (index >= 0) {
            when (text[index]) {
                '@' -> {
                    if (index > 0 && !text[index - 1].isWhitespace()) return null
                    val query = text.substring(index + 1, cursor)
                    if (query.any { it.isWhitespace() }) return null
                    return MentionQuery(startIndex = index, endIndex = cursor, query = query)
                }
                ' ', '\n', '\t' -> return null
            }
            index--
        }
        return null
    }

    fun resolveMentionTargetUserIds(
        mentions: List<MessageMention>,
        memberIds: List<String>,
        senderId: String,
    ): Set<String> {
        if (mentions.isEmpty()) return emptySet()
        val members = memberIds.filter { it.isNotBlank() && it != senderId }.toSet()
        if (mentions.any { it.userId == ALL_USER_ID }) return members
        return mentions
            .map { it.userId }
            .filter { it.isNotBlank() && it != ALL_USER_ID && it in members + memberIds }
            .toSet()
    }

    fun hasAllMention(mentions: List<MessageMention>): Boolean =
        mentions.any { it.userId == ALL_USER_ID }

    fun mentionNotificationBody(
        senderName: String,
        groupName: String,
        messageText: String,
        isAllMention: Boolean,
    ): String {
        val snippet = messageText.take(80)
        return if (isAllMention) {
            "$senderName đã nhắc @All trong $groupName: $snippet"
        } else {
            "$senderName đã nhắc bạn: $snippet"
        }
    }
}

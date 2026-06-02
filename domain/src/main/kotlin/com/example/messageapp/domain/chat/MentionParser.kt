package com.example.messageapp.domain.chat

import com.example.messageapp.domain.model.MessageMention
import com.example.messageapp.domain.model.User
import java.text.Normalizer
import java.util.regex.Pattern

object MentionParser {

    const val ALL_USER_ID = "__all__"
    const val ALL_TOKEN = "All"

    data class MentionCandidate(
        val userId: String,
        val displayName: String,
        val mentionKey: String,
        val avatar: String = "",
        val isAll: Boolean = false,
    )

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

    fun deriveBaseMentionKey(displayName: String): String =
        removeAccent(displayName.lowercase()).replace(Regex("\\s+"), "")

    fun buildMentionCandidates(users: List<User>): List<MentionCandidate> {
        val valid = users.filter { it.keyAuth.isNotBlank() }
        val baseKeys = valid.map { deriveBaseMentionKey(it.name) }
        val keyCounts = baseKeys.groupingBy { it }.eachCount()
        return valid.mapIndexed { index, user ->
            val userId = user.keyAuth
            var key = baseKeys[index]
            if (key.isBlank()) key = userId.takeLast(8)
            if ((keyCounts[key] ?: 0) > 1) key = "${key}_${userId.takeLast(4)}"
            MentionCandidate(
                userId = userId,
                displayName = user.name,
                mentionKey = key,
                avatar = user.avatar,
            )
        }
    }

    fun allMentionCandidate(allLabel: String): MentionCandidate = MentionCandidate(
        userId = ALL_USER_ID,
        displayName = allLabel,
        mentionKey = ALL_TOKEN,
        isAll = true,
    )

    fun insertTextForCandidate(candidate: MentionCandidate): String =
        if (candidate.isAll) ALL_TOKEN else candidate.displayName

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

    fun filterCandidates(
        query: String,
        members: List<MentionCandidate>,
        allCandidate: MentionCandidate,
    ): List<MentionCandidate> {
        val normalizedQuery = removeAccent(query.lowercase())
        val result = mutableListOf<MentionCandidate>()
        val allInsert = removeAccent(ALL_TOKEN.lowercase())
        val allName = removeAccent(allCandidate.displayName.lowercase())
        if (normalizedQuery.isEmpty() ||
            allInsert.startsWith(normalizedQuery) ||
            allName.contains(normalizedQuery)
        ) {
            result.add(allCandidate)
        }
        members.filter { candidate ->
            if (normalizedQuery.isEmpty()) return@filter true
            val key = removeAccent(candidate.mentionKey.lowercase())
            val name = removeAccent(candidate.displayName.lowercase())
            key.startsWith(normalizedQuery) || name.contains(normalizedQuery)
        }.forEach { result.add(it) }
        return result
    }

    fun toMessageMention(candidate: MentionCandidate): MessageMention = MessageMention(
        userId = candidate.userId,
        token = insertTextForCandidate(candidate),
        displayName = candidate.displayName,
    )

    private fun isMentionPresentInText(text: String, mention: MessageMention): Boolean {
        if (mention.token.isBlank()) return false
        if (text.contains("@${mention.token}")) return true
        if (mention.userId == ALL_USER_ID &&
            (text.contains("@$ALL_TOKEN") || text.contains("@all", ignoreCase = true))
        ) {
            return true
        }
        return false
    }

    fun syncPendingMentions(text: String, pending: List<MessageMention>): List<MessageMention> {
        if (pending.isEmpty()) return emptyList()
        return pending.filter { mention -> isMentionPresentInText(text, mention) }
            .distinctBy { it.userId }
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

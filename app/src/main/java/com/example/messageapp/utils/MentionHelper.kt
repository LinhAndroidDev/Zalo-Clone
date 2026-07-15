package com.example.messageapp.utils

import android.content.Context
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.example.messageapp.R
import com.example.messageapp.model.MessageMention
import com.example.messageapp.model.User

object MentionHelper {

    const val ALL_USER_ID = "__all__"
    /** Text inserted after @ for mention-all (displayed as @All). */
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

    fun deriveBaseMentionKey(displayName: String): String {
        return removeAccent(displayName.lowercase()).replace(Regex("\\s+"), "")
    }

    fun buildMentionCandidates(users: List<User>): List<MentionCandidate> {
        val valid = users.filter { !it.keyAuth.isNullOrBlank() }
        val baseKeys = valid.map { deriveBaseMentionKey(it.name.orEmpty()) }
        val keyCounts = baseKeys.groupingBy { it }.eachCount()
        return valid.mapIndexed { index, user ->
            val userId = user.keyAuth.orEmpty()
            var key = baseKeys[index]
            if (key.isBlank()) {
                key = userId.takeLast(8)
            }
            if ((keyCounts[key] ?: 0) > 1) {
                key = "${key}_${userId.takeLast(4)}"
            }
            MentionCandidate(
                userId = userId,
                displayName = user.name.orEmpty(),
                mentionKey = key,
                avatar = user.avatar.orEmpty(),
            )
        }
    }

    fun allMentionCandidate(allLabel: String): MentionCandidate = MentionCandidate(
        userId = ALL_USER_ID,
        displayName = allLabel,
        mentionKey = ALL_TOKEN,
        isAll = true,
    )

    /** Plain text inserted into the message after `@` (member display name or [ALL_TOKEN]). */
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

    fun insertMentionToken(
        editable: Editable,
        query: MentionQuery,
        token: String,
    ): Int {
        val mentionText = "@$token "
        editable.replace(query.startIndex, query.endIndex, mentionText)
        return query.startIndex + mentionText.length
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

    fun syncPendingMentions(
        text: String,
        pending: List<MessageMention>,
    ): List<MessageMention> {
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
        context: Context,
        senderName: String,
        groupName: String,
        messageText: String,
        isAllMention: Boolean,
    ): String {
        val snippet = messageText.take(80)
        return if (isAllMention) {
            context.getString(R.string.notification_mention_all_body, senderName, groupName, snippet)
        } else {
            context.getString(R.string.notification_mention_body, senderName, snippet)
        }
    }

    fun applyMentionSpans(
        context: Context,
        text: String,
        mentions: List<MessageMention>,
    ): CharSequence {
        if (text.isEmpty() || mentions.isEmpty()) return text
        val spannable = SpannableString(text)
        val color = ContextCompat.getColor(context, R.color.color_link)
        mentions.forEach { mention ->
            val needles = buildList {
                if (mention.token.isNotBlank()) add("@${mention.token}")
                if (mention.userId == ALL_USER_ID) {
                    add("@$ALL_TOKEN")
                    add("@all")
                }
            }.distinct()
            needles.forEach { needle ->
                var start = 0
                while (true) {
                    val index = if (needle.equals("@all", ignoreCase = true)) {
                        findIgnoreCase(text, needle, start)
                    } else {
                        text.indexOf(needle, start)
                    }
                    if (index < 0) break
                    spannable.setSpan(
                        ForegroundColorSpan(color),
                        index,
                        index + needle.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    start = index + needle.length
                }
            }
        }
        return spannable
    }

    private fun findIgnoreCase(text: String, needle: String, start: Int): Int {
        if (start >= text.length) return -1
        return text.indexOf(needle, start, ignoreCase = true)
    }

    /** Applies blue highlight to @tokens in the compose field without changing text content. */
    fun applyMentionSpansToEditable(
        context: Context,
        editable: Editable,
        mentions: List<MessageMention>,
    ) {
        editable.getSpans(0, editable.length, ForegroundColorSpan::class.java)
            .forEach { editable.removeSpan(it) }
        val text = editable.toString()
        if (text.isEmpty() || mentions.isEmpty()) return
        val styled = applyMentionSpans(context, text, mentions)
        if (styled is Spannable) {
            styled.getSpans(0, styled.length, ForegroundColorSpan::class.java).forEach { span ->
                val start = styled.getSpanStart(span)
                val end = styled.getSpanEnd(span)
                editable.setSpan(
                    ForegroundColorSpan(span.foregroundColor),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }
}

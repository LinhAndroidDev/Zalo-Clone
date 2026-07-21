package com.example.messageapp.utils

import com.example.messageapp.model.Message
import com.example.messageapp.model.TypeMessage

object ChatMessageSearchHelper {

    data class ChatSearchMatch(
        val message: Message,
        val matchStart: Int,
        val matchEnd: Int,
    )

    fun findMatchMessages(messages: List<Message>, query: String): List<ChatSearchMatch> {
        val normalizedQuery = removeAccent(query.trim().lowercase())
        if (normalizedQuery.isEmpty()) return emptyList()

        return messages.asSequence()
            .filter { TypeMessage.of(it.type) == TypeMessage.MESSAGE }
            .mapNotNull { message ->
                val range = SearchHighlightHelper.findMatchRange(message.message, query) ?: return@mapNotNull null
                ChatSearchMatch(
                    message = message,
                    matchStart = range.first,
                    matchEnd = range.last + 1,
                )
            }
            .toList()
            .asReversed()
    }

    /** @deprecated Inline header search; kept for compatibility if referenced elsewhere. */
    fun findMatches(messages: List<Message>, query: String): List<String> =
        findMatchMessages(messages, query).map { it.message.time.trim() }.filter { it.isNotBlank() }
}

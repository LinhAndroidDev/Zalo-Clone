package com.example.messageapp.utils

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import com.example.messageapp.R
import com.example.messageapp.model.Message
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.utils.MentionHelper.MentionCandidate

object GroupSystemMessageStyle {

    private const val EVENT_ADD = "add"
    private const val EVENT_REMOVE = "remove"
    private const val EVENT_LEAVE = "leave"

    private const val SUFFIX_ADDED = " vào nhóm"
    private const val SUFFIX_REMOVED = " khỏi nhóm"
    private const val SUFFIX_LEFT = " đã rời nhóm"
    private const val MIDDLE_ADDED = " đã thêm "
    private const val MIDDLE_REMOVED = " đã xóa "

    /** Tin nhóm dạng thông báo (type SYSTEM hoặc nội dung legacy trước khi có metadata đầy đủ). */
    fun isGroupSystemMessage(message: Message): Boolean =
        TypeMessage.of(message.type) == TypeMessage.SYSTEM ||
        inferEventFromLegacyText(message.message).isNotBlank()

    fun styledText(
        context: Context,
        message: Message,
        myUserId: String,
        myDisplayName: String = "",
        groupMembers: List<MentionCandidate> = emptyList(),
    ): CharSequence {
        val (text, highlights) = buildDisplay(context, message, myUserId, myDisplayName, groupMembers)
        if (text.isBlank()) return text

        val spannable = SpannableString(text)
        val baseColor = ContextCompat.getColor(context, R.color.text_primary)
        val highlightColor = ContextCompat.getColor(context, R.color.text_primary)

        spannable.setSpan(
            ForegroundColorSpan(baseColor),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        highlights.sortedByDescending { it.length }.forEach { phrase ->
            if (phrase.isBlank()) return@forEach
            var start = 0
            while (start < text.length) {
                val index = text.indexOf(phrase, start, ignoreCase = false)
                if (index < 0) break
                val end = index + phrase.length
                spannable.setSpan(StyleSpan(Typeface.BOLD), index, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(
                    ForegroundColorSpan(highlightColor),
                    index,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                start = end
            }
        }
        return spannable
    }

    private fun buildDisplay(
        context: Context,
        message: Message,
        myUserId: String,
        myDisplayName: String,
        groupMembers: List<MentionCandidate> = emptyList(),
    ): Pair<String, List<String>> {
        val event = message.systemEvent.ifBlank {
            inferEventFromLegacyText(message.message)
        }
        if (!isGroupSystemMessage(message) || event.isBlank()) {
            return message.message to emptyList()
        }

        val actorId = message.systemActorId.ifBlank { message.sender }
        val targetIds = message.systemTargetIds
        val targetNames = message.systemTargetNames
        val actorName = message.systemActorName.ifBlank {
            resolveNameFromLegacyText(message.message, actorId, targetIds, targetNames, isActor = true)
        }

        return when (event) {
            EVENT_ADD -> buildAddDisplay(
                context, myUserId, myDisplayName, actorId, actorName, targetIds, targetNames, message.message, groupMembers,
            )
            EVENT_REMOVE -> buildRemoveDisplay(
                context, myUserId, myDisplayName, actorId, actorName, targetIds, targetNames, message.message, groupMembers,
            )
            EVENT_LEAVE -> buildLeaveDisplay(
                context, myUserId, myDisplayName, targetIds, targetNames, message.message, groupMembers,
            )
            else -> legacyDisplay(
                context, message.message, myUserId, myDisplayName, actorId, actorName, targetIds, targetNames, groupMembers,
            )
        }
    }

    private fun buildAddDisplay(
        context: Context,
        myUserId: String,
        myDisplayName: String,
        actorId: String,
        actorName: String,
        targetIds: List<String>,
        targetNames: List<String>,
        legacyMessage: String = "",
        groupMembers: List<MentionCandidate> = emptyList(),
    ): Pair<String, List<String>> {
        val you = context.getString(R.string.group_system_you)
        val youCapital = context.getString(R.string.group_system_you_capital)
        val resolvedNames = resolveTargetNames(targetIds, targetNames, legacyMessage, groupMembers)
        val targetsLabel = formatDisplayNameList(resolvedNames)
        val actorIsMe = isActorMe(actorId, actorName, myUserId, myDisplayName, groupMembers)
        val targetIsMe = isTargetMe(targetIds, resolvedNames, myUserId, myDisplayName, groupMembers)
        val singleTarget = targetIds.size == 1 || resolvedNames.size == 1

        return when {
            singleTarget && targetIsMe && !actorIsMe -> {
                val text = context.getString(R.string.group_event_added_you_target, actorName)
                text to listOf(you, actorName).filter { it.isNotBlank() }.distinct()
            }

            actorIsMe -> {
                val text = context.getString(R.string.group_event_added_actor_self, targetsLabel)
                val highlights = mutableListOf(youCapital)
                highlights.addAll(resolvedNames.filter { it.isNotBlank() })
                text to highlights.distinct()
            }

            targetIsMe -> {
                val others = othersExcludingMe(targetIds, resolvedNames, myUserId, myDisplayName, groupMembers)
                val othersLabel = formatDisplayNameList(others)
                val text = if (othersLabel.isBlank()) {
                    context.getString(R.string.group_event_added_you_target, actorName)
                } else {
                    context.getString(R.string.group_event_added, actorName, "$you, $othersLabel")
                }
                text to listOf(you, actorName).plus(others).filter { it.isNotBlank() }.distinct()
            }

            else -> {
                val text = context.getString(R.string.group_event_added, actorName, targetsLabel)
                text to (listOf(actorName) + resolvedNames).filter { it.isNotBlank() }.distinct()
            }
        }
    }

    private fun buildRemoveDisplay(
        context: Context,
        myUserId: String,
        myDisplayName: String,
        actorId: String,
        actorName: String,
        targetIds: List<String>,
        targetNames: List<String>,
        legacyMessage: String = "",
        groupMembers: List<MentionCandidate> = emptyList(),
    ): Pair<String, List<String>> {
        val you = context.getString(R.string.group_system_you)
        val youCapital = context.getString(R.string.group_system_you_capital)
        val targetId = targetIds.firstOrNull().orEmpty()
        val targetName = resolveTargetNames(targetIds, targetNames, legacyMessage, groupMembers).firstOrNull().orEmpty()
            .ifBlank {
                if (legacyMessage.contains(MIDDLE_REMOVED)) {
                    legacyMessage.substringAfter(MIDDLE_REMOVED).removeSuffix(SUFFIX_REMOVED).trim()
                } else {
                    ""
                }
            }

        val actorIsMe = isActorMe(actorId, actorName, myUserId, myDisplayName, groupMembers)
        val targetIsMe = isSelf(targetId, myUserId, targetName, myDisplayName) ||
            isNameMine(targetName, myUserId, myDisplayName, groupMembers)

        return when {
            targetIsMe && !actorIsMe -> {
                val text = context.getString(R.string.group_event_removed_you_target, actorName)
                text to listOf(you, actorName).filter { it.isNotBlank() }.distinct()
            }

            actorIsMe -> {
                val text = context.getString(R.string.group_event_removed_actor_self, targetName)
                text to listOf(youCapital, targetName).filter { it.isNotBlank() }
            }

            else -> {
                val text = context.getString(R.string.group_event_removed, actorName, targetName)
                text to listOf(actorName, targetName).filter { it.isNotBlank() }
            }
        }
    }

    private fun buildLeaveDisplay(
        context: Context,
        myUserId: String,
        myDisplayName: String,
        targetIds: List<String>,
        targetNames: List<String>,
        legacyMessage: String,
        groupMembers: List<MentionCandidate> = emptyList(),
    ): Pair<String, List<String>> {
        val youCapital = context.getString(R.string.group_system_you_capital)
        val targetId = targetIds.firstOrNull().orEmpty()
        val targetName = targetNames.firstOrNull().orEmpty().ifBlank {
            legacyMessage.removeSuffix(SUFFIX_LEFT).trim()
        }
        val leaverIsMe = isSelf(targetId, myUserId, targetName, myDisplayName) ||
            isNameMine(targetName, myUserId, myDisplayName, groupMembers)

        return if (leaverIsMe) {
            val text = context.getString(R.string.group_event_left_self)
            text to listOf(youCapital)
        } else {
            val text = context.getString(R.string.group_event_left, targetName)
            text to listOf(targetName).filter { it.isNotBlank() }
        }
    }

    /** Tin SYSTEM cũ không có [Message.systemEvent] — suy luận từ nội dung + sender. */
    private fun legacyDisplay(
        context: Context,
        text: String,
        myUserId: String,
        myDisplayName: String,
        actorId: String,
        actorName: String,
        targetIds: List<String>,
        targetNames: List<String>,
        groupMembers: List<MentionCandidate> = emptyList(),
    ): Pair<String, List<String>> {
        val event = inferEventFromLegacyText(text)
        return when (event) {
            EVENT_ADD -> buildAddDisplay(
                context, myUserId, myDisplayName, actorId, actorName, targetIds, targetNames, text, groupMembers,
            )
            EVENT_REMOVE -> buildRemoveDisplay(
                context, myUserId, myDisplayName, actorId, actorName, targetIds, targetNames, text, groupMembers,
            )
            EVENT_LEAVE -> buildLeaveDisplay(
                context, myUserId, myDisplayName, targetIds, targetNames, text, groupMembers,
            )
            else -> text to highlightNamesFromLegacyText(text)
        }
    }

    private fun inferEventFromLegacyText(text: String): String = when {
        text.contains(MIDDLE_ADDED) && text.endsWith(SUFFIX_ADDED) -> EVENT_ADD
        text.contains(MIDDLE_REMOVED) && text.endsWith(SUFFIX_REMOVED) -> EVENT_REMOVE
        text.endsWith(SUFFIX_LEFT) -> EVENT_LEAVE
        else -> ""
    }

    private fun resolveNameFromLegacyText(
        text: String,
        actorId: String,
        targetIds: List<String>,
        targetNames: List<String>,
        isActor: Boolean,
    ): String {
        if (isActor && text.contains(MIDDLE_ADDED)) {
            return text.substringBefore(MIDDLE_ADDED).trim()
        }
        if (isActor && text.contains(MIDDLE_REMOVED)) {
            return text.substringBefore(MIDDLE_REMOVED).trim()
        }
        if (!isActor && text.endsWith(SUFFIX_LEFT)) {
            return text.removeSuffix(SUFFIX_LEFT).trim()
        }
        val index = targetIds.indexOf(actorId)
        return if (index >= 0 && index < targetNames.size) targetNames[index] else ""
    }

    private fun sameUser(a: String, b: String): Boolean =
        a.isNotBlank() && b.isNotBlank() && a.trim() == b.trim()

    private fun isSelf(
        userId: String,
        myUserId: String,
        displayName: String?,
        myDisplayName: String,
    ): Boolean {
        if (sameUser(userId, myUserId)) return true
        return isNameMine(displayName.orEmpty(), myUserId, myDisplayName, emptyList())
    }

    /** Tin cũ thiếu [systemActorId] / [systemTargetIds] — so khớp theo tên + danh sách thành viên nhóm. */
    private fun isActorMe(
        actorId: String,
        actorName: String,
        myUserId: String,
        myDisplayName: String,
        groupMembers: List<MentionCandidate>,
    ): Boolean = sameUser(actorId, myUserId) || isNameMine(actorName, myUserId, myDisplayName, groupMembers)

    private fun isTargetMe(
        targetIds: List<String>,
        resolvedNames: List<String>,
        myUserId: String,
        myDisplayName: String,
        groupMembers: List<MentionCandidate>,
    ): Boolean {
        if (myUserId.isNotBlank() && targetIds.any { sameUser(it, myUserId) }) return true
        return resolvedNames.any { isNameMine(it, myUserId, myDisplayName, groupMembers) }
    }

    private fun othersExcludingMe(
        targetIds: List<String>,
        resolvedNames: List<String>,
        myUserId: String,
        myDisplayName: String,
        groupMembers: List<MentionCandidate>,
    ): List<String> {
        if (targetIds.isNotEmpty()) {
            return targetIds.zip(resolvedNames)
                .filter { (id, name) -> !isSelf(id, myUserId, name, myDisplayName) }
                .map { it.second }
        }
        return resolvedNames.filter { !isNameMine(it, myUserId, myDisplayName, groupMembers) }
    }

    private fun isNameMine(
        name: String,
        myUserId: String,
        myDisplayName: String,
        groupMembers: List<MentionCandidate>,
    ): Boolean {
        val candidate = name.trim()
        if (candidate.isBlank()) return false
        val mine = myDisplayName.trim()
        if (mine.isNotBlank() && candidate.equals(mine, ignoreCase = true)) return true
        val fromMembers = groupMembers.firstOrNull { sameUser(it.userId, myUserId) }?.displayName?.trim().orEmpty()
        return fromMembers.isNotBlank() && candidate.equals(fromMembers, ignoreCase = true)
    }

    /** Khi metadata [systemTargetNames] trống, lấy tên từ nội dung tin nhắn gốc (vd. "Linh đã thêm Nam vào nhóm"). */
    private fun resolveTargetNames(
        targetIds: List<String>,
        targetNames: List<String>,
        legacyMessage: String,
        groupMembers: List<MentionCandidate> = emptyList(),
    ): List<String> {
        val fromMeta = targetIds.mapIndexed { i, _ ->
            targetNames.getOrNull(i)?.trim()?.takeIf { it.isNotBlank() }
        }
        if (fromMeta.any { !it.isNullOrBlank() }) {
            return targetIds.mapIndexed { i, id ->
                fromMeta.getOrNull(i)?.takeIf { it.isNotBlank() }
                    ?: displayNameForMember(id, groupMembers)
            }
        }
        val parsed = parseTargetsFromLegacyAdd(legacyMessage)
        if (parsed.isNotEmpty()) return parsed
        return targetIds.map { displayNameForMember(it, groupMembers) }
    }

    private fun displayNameForMember(userId: String, groupMembers: List<MentionCandidate>): String =
        groupMembers.firstOrNull { it.userId == userId }?.displayName?.trim().orEmpty()

    private fun parseTargetsFromLegacyAdd(legacyMessage: String): List<String> {
        if (!legacyMessage.contains(MIDDLE_ADDED) || !legacyMessage.endsWith(SUFFIX_ADDED)) return emptyList()
        val part = legacyMessage.substringAfter(MIDDLE_ADDED).removeSuffix(SUFFIX_ADDED).trim()
        return splitDisplayNameList(part)
    }

    private fun formatDisplayNameList(names: List<String>): String {
        val cleaned = names.map { it.trim() }.filter { it.isNotBlank() }
        return when (cleaned.size) {
            0 -> ""
            1 -> cleaned[0]
            2 -> "${cleaned[0]} và ${cleaned[1]}"
            else -> cleaned.dropLast(1).joinToString(", ") + " và ${cleaned.last()}"
        }
    }

    private fun highlightNamesFromLegacyText(text: String): List<String> {
        return when {
            text.contains(MIDDLE_ADDED) && text.endsWith(SUFFIX_ADDED) -> {
                val actor = text.substringBefore(MIDDLE_ADDED).trim()
                val targetsPart = text.substringAfter(MIDDLE_ADDED).removeSuffix(SUFFIX_ADDED).trim()
                listOf(actor) + splitDisplayNameList(targetsPart)
            }

            text.contains(MIDDLE_REMOVED) && text.endsWith(SUFFIX_REMOVED) -> {
                val actor = text.substringBefore(MIDDLE_REMOVED).trim()
                val target = text.substringAfter(MIDDLE_REMOVED).removeSuffix(SUFFIX_REMOVED).trim()
                listOf(actor, target)
            }

            text.endsWith(SUFFIX_LEFT) -> {
                listOf(text.removeSuffix(SUFFIX_LEFT).trim())
            }

            else -> emptyList()
        }.filter { it.isNotBlank() }.distinct()
    }

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

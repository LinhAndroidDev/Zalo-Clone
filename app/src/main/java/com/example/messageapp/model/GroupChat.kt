package com.example.messageapp.model

/** Firestore document at `groups/{groupId}`. */
data class GroupChat(
    val name: String = "",
    val photoUrl: String = "",
    val memberIds: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val typing: Boolean = false,
    val typingUserId: String = "",
)

package com.example.messageapp.domain.model

data class GroupChat(
    val name: String = "",
    val photoUrl: String = "",
    val memberIds: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val typing: Boolean = false,
    val typingUserId: String = "",
)

data class UserPresence(
    val online: Boolean = false,
    val lastSeen: Long = 0L,
)

data class MemberReadCursor(
    val userId: String = "",
    val lastReadTime: String = "",
)

data class User(
    val name: String = "",
    val email: String = "",
    val avatar: String = "",
    val imageCover: String = "",
    val keyAuth: String = "",
)

data class Friend(
    val userId: String = "",
    val name: String = "",
    val avatar: String = "",
)

data class FriendRequest(
    val id: String = "",
    val fromId: String = "",
    val toId: String = "",
    val fromName: String = "",
    val fromAvatar: String = "",
    val toName: String = "",
    val toAvatar: String = "",
    val status: String = "",
    val createdAt: Long = 0L,
)

package com.example.messageapp.model

data class FriendRequest(
    val requestId: String = "",
    val fromId: String = "",
    val toId: String = "",
    val fromName: String = "",
    val fromAvatar: String = "",
    val toName: String = "",
    val toAvatar: String = "",
    val status: String = STATUS_PENDING,
    val createdAt: Long = 0L
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
    }
}

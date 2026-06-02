package com.example.messageapp.domain.model

data class Conversation(
    val friendId: String = "",
    val friendImage: String = "",
    val message: String = "",
    val name: String = "",
    val person: String = "",
    val sender: String = "",
    val time: String = "",
    val seen: String = "0",
    val numberUnSeen: Int = 0,
    val typing: Boolean = false,
    val isGroup: Boolean = false,
) {
    companion object {
        private val GROUP_THREAD_ROOM_ID: Regex =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        fun looksLikeGroupRoomId(friendId: String): Boolean =
            friendId.isNotBlank() && GROUP_THREAD_ROOM_ID.matches(friendId)
    }

    fun isGroupThread(): Boolean = isGroup || looksLikeGroupRoomId(friendId)

}

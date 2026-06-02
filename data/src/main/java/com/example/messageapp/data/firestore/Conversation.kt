package com.example.messageapp.data.firestore

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Conversation(
    val friendId: String = "",
    val friendImage: String = "",
    var message: String = "",
    val name: String = "",
    var person: String = "",
    var sender: String = "",
    var time: String = "",
    var seen: String = "0",
    var numberUnSeen: Int = 0,
    var typing: Boolean = false,
    /**
     * When true, [friendId] is the group document id (room id), not a user id.
     * [PropertyName] on getter + field helps Firestore `toObject`/`set` map the boolean `isGroup`
     * (Kotlin `is*` booleans can otherwise deserialize as false).
     */
    @get:PropertyName("isGroup")
    @field:PropertyName("isGroup")
    val isGroup: Boolean = false,
) : Parcelable {

    companion object {
        /** Same string form as [java.util.UUID.randomUUID] used for group ids in [com.example.messageapp.utils.FireBaseInstance.createGroup]. */
        private val GROUP_THREAD_ROOM_ID: Regex =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        fun looksLikeGroupRoomId(friendId: String): Boolean =
            friendId.isNotBlank() && GROUP_THREAD_ROOM_ID.matches(friendId)
    }

    /**
     * True for a group chat thread: explicit Firestore flag, or [friendId] matches the app’s
     * group room id pattern when the boolean flag failed to deserialize.
     */
    fun isGroupThread(): Boolean = isGroup || looksLikeGroupRoomId(friendId)

    constructor(user: User) : this (
        friendId = user.keyAuth ?: "",
        friendImage = user.avatar ?: "",
        message = "",
        name = user.name ?: "",
        person = "",
        sender = "",
        time = "",
        seen = "0",
    )

    fun isSeenMessage(): Boolean {
        return seen == "1"
    }

    fun isMessageFromFriend(): Boolean {
        return sender == friendId
    }
}
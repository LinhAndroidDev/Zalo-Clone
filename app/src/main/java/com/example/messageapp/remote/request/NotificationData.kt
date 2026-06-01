package com.example.messageapp.remote.request

data class NotificationData(
    val token: String? = null,
    val data: Data? = null
)

data class Data(
    val title: String,
    val body: String,
    val senderId: String,
    /** Present for group chat notifications so the app can open the correct room. */
    val groupId: String? = null,
    val isMention: String? = "0",
    val mentionType: String? = null,
    /** Document id of the message this notification refers to (for inline reply quote). */
    val messageTime: String? = null,
    val replyPreviewText: String? = null,
    val replySenderName: String? = null,
    val replyType: String? = null,
    val replyPhotoUrl: String? = null,
)
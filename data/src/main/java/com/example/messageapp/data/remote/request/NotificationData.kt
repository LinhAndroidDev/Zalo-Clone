package com.example.messageapp.data.remote.request

data class NotificationData(
    val token: String? = null,
    val data: Data? = null,
    /** Cấu hình riêng cho Android (priority/ttl). null = mặc định của FCM. */
    val android: AndroidConfig? = null,
)

/** priority "high" để push tới ngay cả khi máy ở Doze; ttl ví dụ "30s". */
data class AndroidConfig(
    val priority: String = "high",
    val ttl: String? = null,
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
    val diaryNotificationType: String? = null,
    /** User id that should receive this push (chat 1-1, group member, or diary). */
    val recipientUserId: String? = null,
    val postId: String? = null,
    val commentId: String? = null,
    val replyId: String? = null,
    val reactionType: String? = null,
    /** "call" cho push cuộc gọi đến; null với các push khác. */
    val type: String? = null,
    val callId: String? = null,
    /** "voice" hoặc "video". */
    val callType: String? = null,
    val callerName: String? = null,
    val callerAvatar: String? = null,
)
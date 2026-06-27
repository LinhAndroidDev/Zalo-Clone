package com.example.messageapp.model

/**
 * Comment on a diary post (Firestore: posts/{postId}/comments/{commentId}).
 */
data class DiaryPostComment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val text: String,
    val createdAtMillis: Long,
    val likeCount: Int = 0,
    val likedByMe: Boolean = false,
    val replyCount: Int = 0,
    val parentCommentId: String = "",
    val mentionedUserId: String = "",
    val mentionedName: String = "",
)

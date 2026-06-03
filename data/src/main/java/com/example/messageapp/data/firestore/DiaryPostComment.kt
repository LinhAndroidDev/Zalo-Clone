package com.example.messageapp.data.firestore

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
    val createdAtMillis: Long
)

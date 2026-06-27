package com.example.messageapp.data.firestore

/**
 * Diary / status post for UI and Firestore feed.
 * [imageUris] holds remote URLs (https) after sync or local content:// while composing.
 */
data class DiaryPost(
    val id: String,
    val authorUserId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val content: String,
    val imageUris: List<String> = emptyList(),
    val linkPreview: DiaryLinkPreview? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedByMe: Boolean = false,
    val myReactionType: String = "",
    val emotionCounts: Map<String, Int> = emptyMap(),
)

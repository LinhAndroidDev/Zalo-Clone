package com.example.messageapp.model

/**
 * Local-only diary / status post (UI preview; not persisted to Firebase yet).
 */
data class DiaryPost(
    val id: String,
    val authorUserId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val content: String,
    val imageUris: List<String> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis()
)

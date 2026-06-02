package com.example.messageapp.data.firestore

/**
 * Rich link preview for diary posts (Open Graph–style fields stored in Firestore).
 */
data class DiaryLinkPreview(
    val url: String,
    val title: String,
    val description: String,
    val imageUrl: String?
)

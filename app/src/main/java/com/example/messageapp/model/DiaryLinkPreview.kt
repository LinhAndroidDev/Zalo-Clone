package com.example.messageapp.model

/**
 * Rich link preview for diary posts (Open Graph–style fields stored in Firestore).
 */
data class DiaryLinkPreview(
    val url: String,
    val title: String,
    val description: String,
    val imageUrl: String?
)

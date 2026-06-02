package com.example.messageapp.domain.model

data class DiaryLinkPreview(
    val url: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
)

data class DiaryPostComment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val createdAtMillis: Long = 0L,
)

data class DiaryPost(
    val id: String = "",
    val authorUserId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val imageUris: List<String> = emptyList(),
    val linkPreview: DiaryLinkPreview? = null,
    val createdAtMillis: Long = 0L,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedByMe: Boolean = false,
)

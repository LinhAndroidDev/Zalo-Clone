package com.example.messageapp.domain.repository

import com.example.messageapp.domain.model.DiaryLinkPreview
import com.example.messageapp.domain.model.DiaryPost
import com.example.messageapp.domain.model.DiaryPostComment

interface DiaryRepository {
    fun observeFeed(
        userId: String,
        onPosts: (List<DiaryPost>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit

    fun getPost(postId: String, onSuccess: (DiaryPost) -> Unit, onFailure: (String) -> Unit)

    fun createPost(
        authorId: String,
        authorName: String,
        authorAvatarUrl: String,
        content: String,
        localImageUriStrings: List<String>,
        linkPreview: DiaryLinkPreview?,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
    )

    fun updatePost(
        postId: String,
        editorUserId: String,
        content: String,
        localImageUriStrings: List<String>,
        linkPreview: DiaryLinkPreview?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    )

    fun deletePost(postId: String, editorUserId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)

    fun toggleLike(
        postId: String,
        userId: String,
        currentlyLiked: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    )

    fun observeComments(
        postId: String,
        onUpdate: (List<DiaryPostComment>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit

    fun addComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String,
        text: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
    )
}

interface NotificationRepository {
    fun sendPushNotification(token: String, title: String, body: String, data: Map<String, String>)
}

interface StickerRepository {
    fun getStickerUrls(typeName: String, onSuccess: (List<String>) -> Unit)
}

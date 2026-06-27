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

    fun setPostReaction(
        postId: String,
        userId: String,
        authorName: String,
        authorAvatarUrl: String,
        reactionType: com.example.messageapp.domain.model.EmotionType,
        currentReaction: com.example.messageapp.domain.model.EmotionType?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    )

    fun observeComments(
        postId: String,
        userId: String,
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

    fun editComment(postId: String, commentId: String, newText: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun deleteComment(postId: String, commentId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun editReply(postId: String, commentId: String, replyId: String, newText: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun deleteReply(postId: String, commentId: String, replyId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)

    fun toggleCommentLike(
        postId: String,
        commentId: String,
        userId: String,
        currentlyLiked: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    )

    fun observeReplies(
        postId: String,
        commentId: String,
        userId: String,
        onUpdate: (List<DiaryPostComment>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit

    fun addReply(
        postId: String,
        commentId: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String,
        text: String,
        mentionedUserId: String,
        mentionedName: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
    )

    fun toggleReplyLike(
        postId: String,
        commentId: String,
        replyId: String,
        userId: String,
        currentlyLiked: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    )
}

interface NotificationRepository {
    fun sendPushNotification(token: String, title: String, body: String, data: Map<String, String>)
}

interface StickerRepository {
    fun getStickerUrls(typeName: String, onSuccess: (List<String>) -> Unit)
}

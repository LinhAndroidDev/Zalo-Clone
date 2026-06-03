package com.example.messageapp.data.repository

import com.example.messageapp.data.DataContextHolder
import com.example.messageapp.data.legacy.FireBaseInstance
import com.example.messageapp.data.mapper.EntityMapper
import com.example.messageapp.domain.model.DiaryLinkPreview
import com.example.messageapp.domain.model.DiaryPost
import com.example.messageapp.domain.model.DiaryPostComment
import com.example.messageapp.domain.repository.DiaryRepository
import com.example.messageapp.domain.repository.NotificationRepository
import com.example.messageapp.domain.repository.StickerRepository
import com.example.messageapp.data.firestore.Sticker as FsSticker
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class DiaryRepositoryImpl @Inject constructor() : DiaryRepository {

    override fun observeFeed(
        userId: String,
        onPosts: (List<DiaryPost>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit = FireBaseInstance.observeDiaryFeed(
        userId = userId,
        onPosts = { posts -> onPosts(posts.map { EntityMapper.toDomain(it) }) },
        onError = onError,
    )

    override fun getPost(postId: String, onSuccess: (DiaryPost) -> Unit, onFailure: (String) -> Unit) {
        FireBaseInstance.getDiaryPost(
            postId = postId,
            success = { onSuccess(EntityMapper.toDomain(it)) },
            failure = onFailure,
        )
    }

    override fun createPost(
        authorId: String,
        authorName: String,
        authorAvatarUrl: String,
        content: String,
        localImageUriStrings: List<String>,
        linkPreview: DiaryLinkPreview?,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val context = DataContextHolder.appContext
        FireBaseInstance.createDiaryPost(
            context = context,
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            content = content,
            localImageUris = localImageUriStrings.map { it.toUri() },
            linkPreview = linkPreview?.let { EntityMapper.toFirestore(it) },
            success = onSuccess,
            failure = onFailure,
        )
    }

    override fun updatePost(
        postId: String,
        editorUserId: String,
        content: String,
        localImageUriStrings: List<String>,
        linkPreview: DiaryLinkPreview?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val context = DataContextHolder.appContext
        FireBaseInstance.updateDiaryPost(
            context = context,
            postId = postId,
            editorUserId = editorUserId,
            content = content,
            imageUris = localImageUriStrings.map { it.toUri() },
            linkPreview = linkPreview?.let { EntityMapper.toFirestore(it) },
            success = onSuccess,
            failure = onFailure,
        )
    }

    override fun deletePost(
        postId: String,
        editorUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.deleteDiaryPost(postId, editorUserId, onSuccess, onFailure)
    }

    override fun toggleLike(
        postId: String,
        userId: String,
        currentlyLiked: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.toggleDiaryPostLike(
            postId = postId,
            userId = userId,
            currentlyLiked = currentlyLiked,
            success = onSuccess,
            failure = onFailure,
        )
    }

    override fun observeComments(
        postId: String,
        onUpdate: (List<DiaryPostComment>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit {
        val registration = FireBaseInstance.observeDiaryComments(
            postId = postId,
            onUpdate = { comments -> onUpdate(comments.map { EntityMapper.toDomain(it) }) },
            onError = onError,
        )
        return { registration.remove() }
    }

    override fun addComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String,
        text: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        FireBaseInstance.addDiaryComment(
            postId = postId,
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            text = text,
            success = onSuccess,
            failure = onFailure,
        )
    }
}

@Singleton
class NotificationRepositoryImpl @Inject constructor() : NotificationRepository {
    override fun sendPushNotification(token: String, title: String, body: String, data: Map<String, String>) {
        // Reserved — push is sent from legacy chat send path today.
    }
}

@Singleton
class StickerRepositoryImpl @Inject constructor() : StickerRepository {
    override fun getStickerUrls(typeName: String, onSuccess: (List<String>) -> Unit) {
        val sticker = runCatching { FsSticker.valueOf(typeName) }.getOrElse { FsSticker.HELLO }
        FireBaseInstance.getSticker(sticker, onSuccess)
    }
}

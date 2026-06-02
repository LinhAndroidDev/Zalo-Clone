package com.example.messageapp.data.repository

import android.net.Uri
import com.example.messageapp.data.DataContextHolder
import com.example.messageapp.data.legacy.FireBaseInstance
import com.example.messageapp.domain.repository.DiaryRepository
import com.example.messageapp.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepositoryImpl @Inject constructor() : DiaryRepository {
    override fun createPost(
        authorId: String,
        content: String,
        imageUris: List<String>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val context = DataContextHolder.appContext
        FireBaseInstance.createDiaryPost(
            context = context,
            authorId = authorId,
            authorName = "",
            authorAvatarUrl = "",
            content = content,
            localImageUris = imageUris.map { Uri.parse(it) },
            success = { onSuccess() },
            failure = onFailure,
        )
    }

    override fun deletePost(postId: String, authorId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        FireBaseInstance.deleteDiaryPost(postId, authorId, onSuccess, onFailure)
    }

    override fun observeFeed(userId: String, onUpdate: () -> Unit): () -> Unit =
        FireBaseInstance.observeDiaryFeed(userId, onPosts = { onUpdate() }, onError = { })
}

@Singleton
class NotificationRepositoryImpl @Inject constructor() : NotificationRepository {
    override fun sendPushNotification(token: String, title: String, body: String, data: Map<String, String>) {
        // Reserved — push is sent from legacy chat send path today.
    }
}

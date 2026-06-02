package com.example.messageapp.domain.repository

interface DiaryRepository {
    fun createPost(
        authorId: String,
        content: String,
        imageUris: List<String>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    )
    fun deletePost(postId: String, authorId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
    fun observeFeed(userId: String, onUpdate: () -> Unit): () -> Unit
}

interface NotificationRepository {
    fun sendPushNotification(token: String, title: String, body: String, data: Map<String, String>)
}

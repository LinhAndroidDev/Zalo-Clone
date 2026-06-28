package com.example.messageapp.domain.repository

import com.example.messageapp.domain.model.DiaryNotification

interface DiaryNotificationRepository {
    fun observeNotifications(
        userId: String,
        onUpdate: (List<DiaryNotification>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit

    fun markAsRead(userId: String, notificationId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)

    fun markAllAsRead(userId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit)
}

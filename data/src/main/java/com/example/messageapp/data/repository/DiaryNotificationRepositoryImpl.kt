package com.example.messageapp.data.repository

import com.example.messageapp.data.legacy.FireBaseInstance
import com.example.messageapp.domain.repository.DiaryNotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryNotificationRepositoryImpl @Inject constructor() : DiaryNotificationRepository {

    override fun observeNotifications(
        userId: String,
        onUpdate: (List<com.example.messageapp.domain.model.DiaryNotification>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit = FireBaseInstance.observeDiaryNotifications(userId, onUpdate, onError)

    override fun markAsRead(
        userId: String,
        notificationId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) = FireBaseInstance.markDiaryNotificationRead(userId, notificationId, onSuccess, onFailure)

    override fun markAllAsRead(
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) = FireBaseInstance.markAllDiaryNotificationsRead(userId, onSuccess, onFailure)
}

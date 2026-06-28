package com.example.messageapp.domain.usecase.diary

import com.example.messageapp.domain.model.DiaryNotification
import com.example.messageapp.domain.repository.DiaryNotificationRepository
import com.example.messageapp.domain.repository.SessionRepository
import javax.inject.Inject

class ObserveDiaryNotificationsUseCase @Inject constructor(
    private val repository: DiaryNotificationRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        onUpdate: (List<DiaryNotification>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit = repository.observeNotifications(
        userId = sessionRepository.getAuth(),
        onUpdate = onUpdate,
        onError = onError,
    )
}

class MarkDiaryNotificationReadUseCase @Inject constructor(
    private val repository: DiaryNotificationRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(notificationId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        repository.markAsRead(sessionRepository.getAuth(), notificationId, onSuccess, onFailure)
    }
}

class MarkAllDiaryNotificationsReadUseCase @Inject constructor(
    private val repository: DiaryNotificationRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        repository.markAllAsRead(sessionRepository.getAuth(), onSuccess, onFailure)
    }
}

class ObserveDiaryNotificationUnreadCountUseCase @Inject constructor(
    private val repository: DiaryNotificationRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        onUpdate: (Int) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit = repository.observeNotifications(
        userId = sessionRepository.getAuth(),
        onUpdate = { list -> onUpdate(list.count { !it.read }) },
        onError = onError,
    )
}

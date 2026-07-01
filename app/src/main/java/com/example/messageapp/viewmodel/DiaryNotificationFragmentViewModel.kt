package com.example.messageapp.viewmodel

import android.content.Context
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.usecase.diary.MarkDiaryNotificationReadUseCase
import com.example.messageapp.domain.usecase.diary.ObserveDiaryNotificationsUseCase
import com.example.messageapp.mapper.DiaryNotificationUiMapper
import com.example.messageapp.model.DiaryNotificationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DiaryNotificationFragmentViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val observeDiaryNotificationsUseCase: ObserveDiaryNotificationsUseCase,
    private val markDiaryNotificationReadUseCase: MarkDiaryNotificationReadUseCase,
) : BaseViewModel() {

    private val _notifications = MutableStateFlow<List<DiaryNotificationItem>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private var stopObserve: (() -> Unit)? = null

    fun startObserving() {
        stopObserve?.invoke()
        stopObserve = observeDiaryNotificationsUseCase(
            onUpdate = { list ->
                _notifications.value = DiaryNotificationUiMapper.toUiItems(appContext, list)
            },
            onError = { showError(it) },
        )
    }

    fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) return
        markDiaryNotificationReadUseCase(
            notificationId = notificationId,
            onSuccess = {},
            onFailure = { showError(it) },
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopObserve?.invoke()
        stopObserve = null
    }
}

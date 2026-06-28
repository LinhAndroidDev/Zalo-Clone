package com.example.messageapp.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.diary.MarkAllDiaryNotificationsReadUseCase
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
    private val markAllDiaryNotificationsReadUseCase: MarkAllDiaryNotificationsReadUseCase,
    private val sessionRepository: SessionRepository,
) : BaseViewModel() {

    private val _notifications = MutableStateFlow<List<DiaryNotificationItem>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private var stopObserve: (() -> Unit)? = null

    fun startObserving() {
        sessionRepository.getAuth().ifBlank { return }
        stopObserve?.invoke()
        stopObserve = observeDiaryNotificationsUseCase(
            onUpdate = { list ->
                _notifications.value = DiaryNotificationUiMapper.toUiItems(appContext, list)
            },
            onError = { showError(it) },
        )
    }

    fun markAllAsRead() {
        markAllDiaryNotificationsReadUseCase(
            onSuccess = {
                sessionRepository.saveLastSeenDiaryNotificationAt(System.currentTimeMillis())
            },
            onFailure = { showError(it) },
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopObserve?.invoke()
        stopObserve = null
    }
}

package com.example.messageapp.viewmodel

import android.os.SystemClock
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.diary.GetDiaryAuthorUseCase
import com.example.messageapp.domain.usecase.diary.ObserveDiaryFeedUseCase
import com.example.messageapp.domain.usecase.diary.ToggleDiaryPostLikeUseCase
import com.example.messageapp.mapper.DiaryUiMapper
import com.example.messageapp.mapper.SocialUiMapper
import com.example.messageapp.model.DiaryPost
import com.example.messageapp.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DiaryFragmentViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getDiaryAuthorUseCase: GetDiaryAuthorUseCase,
    private val observeDiaryFeedUseCase: ObserveDiaryFeedUseCase,
    private val toggleDiaryPostLikeUseCase: ToggleDiaryPostLikeUseCase,
    private val deleteDiaryPostUseCase: com.example.messageapp.domain.usecase.diary.DeleteDiaryPostUseCase,
) : BaseViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    private val _diaryPosts = MutableStateFlow<List<DiaryPost>>(emptyList())
    val diaryPosts = _diaryPosts.asStateFlow()

    private var stopFeed: (() -> Unit)? = null
    private var lastDiaryFeedErrorAtMs = 0L

    fun currentUserId(): String = sessionRepository.getAuth()

    fun getInfoUser() {
        sessionRepository.getAuth().ifBlank { return }
        getDiaryAuthorUseCase(onSuccess = { u ->
            _user.value = SocialUiMapper.toUi(u)
        })
    }

    fun startDiaryFeed() {
        sessionRepository.getAuth().ifBlank { return }
        stopFeed?.invoke()
        stopFeed = observeDiaryFeedUseCase(
            onPosts = { list -> _diaryPosts.value = DiaryUiMapper.toUiPosts(list) },
            onError = { msg ->
                val now = SystemClock.elapsedRealtime()
                if (now - lastDiaryFeedErrorAtMs >= 4_000L) {
                    lastDiaryFeedErrorAtMs = now
                    showError(msg)
                }
            },
        )
    }

    fun toggleDiaryPostLike(post: DiaryPost) {
        toggleDiaryPostLikeUseCase(
            post = DiaryUiMapper.toDomain(post),
            onSuccess = {},
            onFailure = { showError(it) },
        )
    }

    fun deleteDiaryPost(postId: String, onSuccess: () -> Unit) {
        deleteDiaryPostUseCase(
            postId = postId,
            onSuccess = onSuccess,
            onFailure = { showError(it) },
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopFeed?.invoke()
        stopFeed = null
    }
}

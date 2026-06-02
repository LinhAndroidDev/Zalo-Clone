package com.example.messageapp.viewmodel

import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.diary.AddDiaryCommentUseCase
import com.example.messageapp.domain.usecase.diary.ObserveDiaryCommentsUseCase
import com.example.messageapp.mapper.DiaryUiMapper
import com.example.messageapp.model.DiaryPostComment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class BottomSheetDiaryCommentsViewModel @Inject constructor(
    private val observeDiaryCommentsUseCase: ObserveDiaryCommentsUseCase,
    private val addDiaryCommentUseCase: AddDiaryCommentUseCase,
    private val sessionRepository: SessionRepository,
) : BaseViewModel() {

    private var stopComments: (() -> Unit)? = null

    private val _comments = MutableStateFlow<List<DiaryPostComment>>(emptyList())
    val comments = _comments.asStateFlow()

    fun startComments(postId: String) {
        stopComments?.invoke()
        stopComments = observeDiaryCommentsUseCase(
            postId = postId,
            onUpdate = { list -> _comments.value = list.map { DiaryUiMapper.toUi(it) } },
            onError = { showError(it) },
        )
    }

    fun sendComment(postId: String, text: String, onSuccess: () -> Unit) {
        addDiaryCommentUseCase(
            postId = postId,
            text = text,
            onSuccess = onSuccess,
            onFailure = { showError(it) },
        )
    }

    fun currentUserId(): String = sessionRepository.getAuth()

    fun currentUserName(): String = sessionRepository.getNameUser()

    override fun onCleared() {
        stopComments?.invoke()
        stopComments = null
        super.onCleared()
    }
}

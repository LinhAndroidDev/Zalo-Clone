package com.example.messageapp.viewmodel

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.diary.CreateDiaryPostUseCase
import com.example.messageapp.domain.usecase.diary.GetDiaryPostUseCase
import com.example.messageapp.domain.usecase.diary.UpdateDiaryPostUseCase
import com.example.messageapp.mapper.DiaryUiMapper
import com.example.messageapp.model.DiaryLinkPreview
import com.example.messageapp.model.DiaryPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatusFragmentViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getDiaryPostUseCase: GetDiaryPostUseCase,
    private val createDiaryPostUseCase: CreateDiaryPostUseCase,
    private val updateDiaryPostUseCase: UpdateDiaryPostUseCase,
) : BaseViewModel() {

    private val _loadedPost = MutableStateFlow<DiaryPost?>(null)
    val loadedPost = _loadedPost.asStateFlow()

    fun currentUserId(): String = sessionRepository.getAuth()

    fun loadPostForEdit(
        postId: String,
        onSuccess: (DiaryPost) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        getDiaryPostUseCase(
            postId = postId,
            onSuccess = { post ->
                val uiPost = DiaryUiMapper.toUi(post)
                _loadedPost.value = uiPost
                onSuccess(uiPost)
            },
            onFailure = onFailure,
        )
    }

    fun createPost(
        content: String,
        imageUris: List<Uri>,
        linkPreview: DiaryLinkPreview?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) = viewModelScope.launch {
        createDiaryPostUseCase(
            content = content,
            localImageUriStrings = imageUris.map { it.toString() },
            linkPreview = DiaryUiMapper.toDomain(linkPreview),
            onSuccess = onSuccess,
            onFailure = onFailure,
        )
    }

    fun updatePost(
        postId: String,
        content: String,
        imageUris: List<Uri>,
        linkPreview: DiaryLinkPreview?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) = viewModelScope.launch {
        updateDiaryPostUseCase(
            postId = postId,
            content = content,
            localImageUriStrings = imageUris.map { it.toString() },
            linkPreview = DiaryUiMapper.toDomain(linkPreview),
            onSuccess = onSuccess,
            onFailure = onFailure,
        )
    }
}

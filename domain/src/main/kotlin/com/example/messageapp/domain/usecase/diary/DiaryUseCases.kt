package com.example.messageapp.domain.usecase.diary

import com.example.messageapp.domain.model.DiaryLinkPreview
import com.example.messageapp.domain.model.DiaryPost
import com.example.messageapp.domain.model.DiaryPostComment
import com.example.messageapp.domain.repository.DiaryRepository
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.repository.UserRepository
import javax.inject.Inject

class ObserveDiaryFeedUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        onPosts: (List<DiaryPost>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit = diaryRepository.observeFeed(sessionRepository.getAuth(), onPosts, onError)
}

class ToggleDiaryPostLikeUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(post: DiaryPost, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        diaryRepository.toggleLike(
            postId = post.id,
            userId = sessionRepository.getAuth(),
            currentlyLiked = post.likedByMe,
            onSuccess = onSuccess,
            onFailure = onFailure,
        )
    }
}

class GetDiaryPostUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
) {
    operator fun invoke(postId: String, onSuccess: (DiaryPost) -> Unit, onFailure: (String) -> Unit) {
        diaryRepository.getPost(postId, onSuccess, onFailure)
    }
}

class CreateDiaryPostUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        content: String,
        localImageUriStrings: List<String>,
        linkPreview: DiaryLinkPreview?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val uid = sessionRepository.getAuth()
        userRepository.getUserById(
            userId = uid,
            onSuccess = { user ->
                diaryRepository.createPost(
                    authorId = uid,
                    authorName = user.name.ifBlank { sessionRepository.getNameUser() },
                    authorAvatarUrl = user.avatar,
                    content = content,
                    localImageUriStrings = localImageUriStrings,
                    linkPreview = linkPreview,
                    onSuccess = { onSuccess() },
                    onFailure = onFailure,
                )
            },
            onFailure = onFailure,
        )
    }
}

class UpdateDiaryPostUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        postId: String,
        content: String,
        localImageUriStrings: List<String>,
        linkPreview: DiaryLinkPreview?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        diaryRepository.updatePost(
            postId = postId,
            editorUserId = sessionRepository.getAuth(),
            content = content,
            localImageUriStrings = localImageUriStrings,
            linkPreview = linkPreview,
            onSuccess = onSuccess,
            onFailure = onFailure,
        )
    }
}

class ObserveDiaryCommentsUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
) {
    operator fun invoke(
        postId: String,
        onUpdate: (List<DiaryPostComment>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit = diaryRepository.observeComments(postId, onUpdate, onError)
}

class AddDiaryCommentUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        postId: String,
        text: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val uid = sessionRepository.getAuth()
        userRepository.getUserById(
            userId = uid,
            onSuccess = { user ->
                diaryRepository.addComment(
                    postId = postId,
                    authorId = uid,
                    authorName = user.name.ifBlank { sessionRepository.getNameUser() },
                    authorAvatarUrl = user.avatar,
                    text = text,
                    onSuccess = { onSuccess() },
                    onFailure = onFailure,
                )
            },
            onFailure = onFailure,
        )
    }
}

class GetDiaryAuthorUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(onSuccess: (com.example.messageapp.domain.model.User) -> Unit) {
        userRepository.getInfoUser(sessionRepository.getAuth(), onSuccess)
    }
}

class DeleteDiaryPostUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(postId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        diaryRepository.deletePost(postId, sessionRepository.getAuth(), onSuccess, onFailure)
    }
}

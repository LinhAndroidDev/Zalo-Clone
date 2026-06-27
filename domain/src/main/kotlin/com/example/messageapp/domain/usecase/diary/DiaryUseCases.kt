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
    private val userRepository: com.example.messageapp.domain.repository.UserRepository,
) {
    operator fun invoke(post: DiaryPost, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val uid = sessionRepository.getAuth()
        val current = if (post.likedByMe) {
            post.myReactionType ?: com.example.messageapp.domain.model.EmotionType.LIKE
        } else {
            null
        }
        userRepository.getUserById(
            userId = uid,
            onSuccess = { user ->
                diaryRepository.setPostReaction(
                    postId = post.id,
                    userId = uid,
                    authorName = user.name.ifBlank { sessionRepository.getNameUser() },
                    authorAvatarUrl = user.avatar,
                    reactionType = com.example.messageapp.domain.model.EmotionType.LIKE,
                    currentReaction = current,
                    onSuccess = onSuccess,
                    onFailure = onFailure,
                )
            },
            onFailure = onFailure,
        )
    }
}

class SetDiaryPostReactionUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: com.example.messageapp.domain.repository.UserRepository,
) {
    operator fun invoke(
        post: DiaryPost,
        reactionType: com.example.messageapp.domain.model.EmotionType,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val uid = sessionRepository.getAuth()
        userRepository.getUserById(
            userId = uid,
            onSuccess = { user ->
                diaryRepository.setPostReaction(
                    postId = post.id,
                    userId = uid,
                    authorName = user.name.ifBlank { sessionRepository.getNameUser() },
                    authorAvatarUrl = user.avatar,
                    reactionType = reactionType,
                    currentReaction = post.myReactionType,
                    onSuccess = onSuccess,
                    onFailure = onFailure,
                )
            },
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
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        postId: String,
        onUpdate: (List<DiaryPostComment>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit = diaryRepository.observeComments(
        postId = postId,
        userId = sessionRepository.getAuth(),
        onUpdate = onUpdate,
        onError = onError,
    )
}

class EditDiaryCommentUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(postId: String, commentId: String, newText: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) =
        diaryRepository.editComment(postId, commentId, newText, onSuccess, onFailure)
}

class DeleteDiaryCommentUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(postId: String, commentId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) =
        diaryRepository.deleteComment(postId, commentId, onSuccess, onFailure)
}

class EditDiaryReplyUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
) {
    operator fun invoke(postId: String, commentId: String, replyId: String, newText: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) =
        diaryRepository.editReply(postId, commentId, replyId, newText, onSuccess, onFailure)
}

class DeleteDiaryReplyUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
) {
    operator fun invoke(postId: String, commentId: String, replyId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) =
        diaryRepository.deleteReply(postId, commentId, replyId, onSuccess, onFailure)
}

class ToggleDiaryCommentLikeUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        postId: String,
        commentId: String,
        currentlyLiked: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        diaryRepository.toggleCommentLike(
            postId = postId,
            commentId = commentId,
            userId = sessionRepository.getAuth(),
            currentlyLiked = currentlyLiked,
            onSuccess = onSuccess,
            onFailure = onFailure,
        )
    }
}

class ObserveDiaryRepliesUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        postId: String,
        commentId: String,
        onUpdate: (List<DiaryPostComment>) -> Unit,
        onError: (String) -> Unit,
    ): () -> Unit = diaryRepository.observeReplies(
        postId = postId,
        commentId = commentId,
        userId = sessionRepository.getAuth(),
        onUpdate = onUpdate,
        onError = onError,
    )
}

class AddDiaryReplyUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        postId: String,
        commentId: String,
        text: String,
        mentionedUserId: String,
        mentionedName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val uid = sessionRepository.getAuth()
        userRepository.getUserById(
            userId = uid,
            onSuccess = { user ->
                diaryRepository.addReply(
                    postId = postId,
                    commentId = commentId,
                    authorId = uid,
                    authorName = user.name.ifBlank { sessionRepository.getNameUser() },
                    authorAvatarUrl = user.avatar,
                    text = text,
                    mentionedUserId = mentionedUserId,
                    mentionedName = mentionedName,
                    onSuccess = { onSuccess() },
                    onFailure = onFailure,
                )
            },
            onFailure = onFailure,
        )
    }
}

class ToggleDiaryReplyLikeUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(
        postId: String,
        commentId: String,
        replyId: String,
        currentlyLiked: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        diaryRepository.toggleReplyLike(
            postId = postId,
            commentId = commentId,
            replyId = replyId,
            userId = sessionRepository.getAuth(),
            currentlyLiked = currentlyLiked,
            onSuccess = onSuccess,
            onFailure = onFailure,
        )
    }
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

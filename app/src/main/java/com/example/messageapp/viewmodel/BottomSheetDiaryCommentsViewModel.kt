package com.example.messageapp.viewmodel

import com.example.messageapp.base.BaseViewModel
import com.example.messageapp.domain.repository.SessionRepository
import com.example.messageapp.domain.usecase.diary.AddDiaryCommentUseCase
import com.example.messageapp.domain.usecase.diary.AddDiaryReplyUseCase
import com.example.messageapp.domain.usecase.diary.DeleteDiaryCommentUseCase
import com.example.messageapp.domain.usecase.diary.DeleteDiaryReplyUseCase
import com.example.messageapp.domain.usecase.diary.EditDiaryCommentUseCase
import com.example.messageapp.domain.usecase.diary.EditDiaryReplyUseCase
import com.example.messageapp.domain.usecase.diary.ObserveDiaryCommentsUseCase
import com.example.messageapp.domain.usecase.diary.ObserveDiaryRepliesUseCase
import com.example.messageapp.domain.usecase.diary.ToggleDiaryCommentLikeUseCase
import com.example.messageapp.domain.usecase.diary.ToggleDiaryReplyLikeUseCase
import com.example.messageapp.mapper.DiaryUiMapper
import com.example.messageapp.model.DiaryCommentRow
import com.example.messageapp.model.DiaryPostComment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class BottomSheetDiaryCommentsViewModel @Inject constructor(
    private val observeDiaryCommentsUseCase: ObserveDiaryCommentsUseCase,
    private val addDiaryCommentUseCase: AddDiaryCommentUseCase,
    private val toggleDiaryCommentLikeUseCase: ToggleDiaryCommentLikeUseCase,
    private val editDiaryCommentUseCase: EditDiaryCommentUseCase,
    private val deleteDiaryCommentUseCase: DeleteDiaryCommentUseCase,
    private val observeDiaryRepliesUseCase: ObserveDiaryRepliesUseCase,
    private val addDiaryReplyUseCase: AddDiaryReplyUseCase,
    private val toggleDiaryReplyLikeUseCase: ToggleDiaryReplyLikeUseCase,
    private val editDiaryReplyUseCase: EditDiaryReplyUseCase,
    private val deleteDiaryReplyUseCase: DeleteDiaryReplyUseCase,
    private val sessionRepository: SessionRepository,
) : BaseViewModel() {

    data class ReplyingTo(
        val commentId: String,
        val mentionedUserId: String,
        val mentionedName: String,
    )

    private var postId: String = ""

    private var stopComments: (() -> Unit)? = null
    private val replyListeners = mutableMapOf<String, () -> Unit>()

    private var comments: List<DiaryPostComment> = emptyList()
    private val repliesByComment = mutableMapOf<String, List<DiaryPostComment>>()
    private val expandedCommentIds = mutableSetOf<String>()
    private val loadingReplyIds = mutableSetOf<String>()

    private var focusCommentId: String = ""
    private var focusReplyId: String = ""
    private var scrollTargetEmitted = false

    private val _rows = MutableStateFlow<List<DiaryCommentRow>>(emptyList())
    val rows = _rows.asStateFlow()

    private val _scrollToRowId = MutableStateFlow<String?>(null)
    val scrollToRowId = _scrollToRowId.asStateFlow()

    private val _replyingTo = MutableStateFlow<ReplyingTo?>(null)
    val replyingTo = _replyingTo.asStateFlow()

    fun startComments(postId: String) {
        this.postId = postId
        stopComments?.invoke()
        stopComments = observeDiaryCommentsUseCase(
            postId = postId,
            onUpdate = { list ->
                comments = list.map { DiaryUiMapper.toUi(it) }
                rebuildRows()
            },
            onError = { showError(it) },
        )
    }

    private fun rebuildRows() {
        val result = mutableListOf<DiaryCommentRow>()
        comments.forEach { comment ->
            result += DiaryCommentRow.CommentRow(comment)
            val expanded = comment.id in expandedCommentIds
            if (comment.replyCount > 0 || expanded) {
                result += DiaryCommentRow.ToggleRepliesRow(
                    commentId = comment.id,
                    replyCount = comment.replyCount,
                    expanded = expanded,
                    loading = comment.id in loadingReplyIds,
                )
            }
            if (expanded) {
                repliesByComment[comment.id].orEmpty().forEach { reply ->
                    result += DiaryCommentRow.ReplyRow(reply)
                }
            }
        }
        _rows.value = result
        tryEmitScrollTarget()
    }

    fun focusTarget(commentId: String, replyId: String) {
        focusCommentId = commentId.trim()
        focusReplyId = replyId.trim()
        scrollTargetEmitted = false
        if (focusCommentId.isBlank()) return
        if (focusReplyId.isNotBlank()) {
            expandReplies(focusCommentId)
        } else {
            tryEmitScrollTarget()
        }
    }

    fun clearScrollTarget() {
        _scrollToRowId.value = null
    }

    private fun tryEmitScrollTarget() {
        if (scrollTargetEmitted || focusCommentId.isBlank()) return
        val targetRowId = if (focusReplyId.isNotBlank()) {
            val replies = repliesByComment[focusCommentId].orEmpty()
            if (replies.none { it.id == focusReplyId }) return
            "reply:$focusCommentId:$focusReplyId"
        } else {
            if (comments.none { it.id == focusCommentId }) return
            "comment:$focusCommentId"
        }
        if (_rows.value.none { it.rowId == targetRowId }) return
        scrollTargetEmitted = true
        _scrollToRowId.value = targetRowId
    }

    private fun expandReplies(commentId: String) {
        if (commentId in expandedCommentIds) {
            tryEmitScrollTarget()
            return
        }
        expandedCommentIds.add(commentId)
        loadingReplyIds.add(commentId)
        rebuildRows()
        replyListeners[commentId] = observeDiaryRepliesUseCase(
            postId = postId,
            commentId = commentId,
            onUpdate = { list ->
                repliesByComment[commentId] = list.map { DiaryUiMapper.toUi(it) }
                loadingReplyIds.remove(commentId)
                rebuildRows()
            },
            onError = {
                loadingReplyIds.remove(commentId)
                showError(it)
            },
        )
    }

    fun toggleReplies(commentId: String) {
        if (commentId in expandedCommentIds) {
            expandedCommentIds.remove(commentId)
            replyListeners.remove(commentId)?.invoke()
            loadingReplyIds.remove(commentId)
            rebuildRows()
            return
        }
        expandReplies(commentId)
    }

    fun toggleCommentLike(comment: DiaryPostComment) {
        val liked = comment.likedByMe
        comments = comments.map {
            if (it.id == comment.id) {
                it.copy(
                    likedByMe = !liked,
                    likeCount = (it.likeCount + if (liked) -1 else 1).coerceAtLeast(0),
                )
            } else {
                it
            }
        }
        rebuildRows()
        toggleDiaryCommentLikeUseCase(
            postId = comment.postId,
            commentId = comment.id,
            currentlyLiked = liked,
            onSuccess = {},
            onFailure = { showError(it) },
        )
    }

    fun toggleReplyLike(reply: DiaryPostComment) {
        val liked = reply.likedByMe
        val parentId = reply.parentCommentId
        repliesByComment[parentId] = repliesByComment[parentId].orEmpty().map {
            if (it.id == reply.id) {
                it.copy(
                    likedByMe = !liked,
                    likeCount = (it.likeCount + if (liked) -1 else 1).coerceAtLeast(0),
                )
            } else {
                it
            }
        }
        rebuildRows()
        toggleDiaryReplyLikeUseCase(
            postId = reply.postId,
            commentId = parentId,
            replyId = reply.id,
            currentlyLiked = liked,
            onSuccess = {},
            onFailure = { showError(it) },
        )
    }

    /** Trả lời một comment gốc: mention chính tác giả comment đó. */
    fun startReplyToComment(comment: DiaryPostComment) {
        _replyingTo.value = ReplyingTo(
            commentId = comment.id,
            mentionedUserId = comment.authorId,
            mentionedName = comment.authorName,
        )
    }

    /** Trả lời một reply: post dưới cùng comment gốc (1 cấp) nhưng mention tác giả reply. */
    fun startReplyToReply(reply: DiaryPostComment) {
        _replyingTo.value = ReplyingTo(
            commentId = reply.parentCommentId,
            mentionedUserId = reply.authorId,
            mentionedName = reply.authorName,
        )
    }

    fun clearReply() {
        _replyingTo.value = null
    }

    fun send(text: String, onSuccess: () -> Unit) {
        val target = _replyingTo.value
        if (target != null) {
            if (target.commentId !in expandedCommentIds) expandReplies(target.commentId)
            addDiaryReplyUseCase(
                postId = postId,
                commentId = target.commentId,
                text = text,
                mentionedUserId = target.mentionedUserId,
                mentionedName = target.mentionedName,
                onSuccess = {
                    clearReply()
                    onSuccess()
                },
                onFailure = { showError(it) },
            )
        } else {
            addDiaryCommentUseCase(
                postId = postId,
                text = text,
                onSuccess = onSuccess,
                onFailure = { showError(it) },
            )
        }
    }

    fun editComment(comment: DiaryPostComment, newText: String, onSuccess: () -> Unit) {
        editDiaryCommentUseCase(
            postId = comment.postId,
            commentId = comment.id,
            newText = newText,
            onSuccess = onSuccess,
            onFailure = { showError(it) },
        )
    }

    fun deleteComment(comment: DiaryPostComment, onSuccess: () -> Unit) {
        deleteDiaryCommentUseCase(
            postId = comment.postId,
            commentId = comment.id,
            onSuccess = onSuccess,
            onFailure = { showError(it) },
        )
    }

    fun editReply(reply: DiaryPostComment, newText: String, onSuccess: () -> Unit) {
        editDiaryReplyUseCase(
            postId = reply.postId,
            commentId = reply.parentCommentId,
            replyId = reply.id,
            newText = newText,
            onSuccess = onSuccess,
            onFailure = { showError(it) },
        )
    }

    fun deleteReply(reply: DiaryPostComment, onSuccess: () -> Unit) {
        deleteDiaryReplyUseCase(
            postId = reply.postId,
            commentId = reply.parentCommentId,
            replyId = reply.id,
            onSuccess = onSuccess,
            onFailure = { showError(it) },
        )
    }

    fun currentUserId(): String = sessionRepository.getAuth()

    fun currentUserName(): String = sessionRepository.getNameUser()

    override fun onCleared() {
        stopComments?.invoke()
        stopComments = null
        replyListeners.values.forEach { it.invoke() }
        replyListeners.clear()
        super.onCleared()
    }
}

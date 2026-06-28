package com.example.messageapp.model

/**
 * Hàng hiển thị phẳng trong danh sách bình luận của nhật ký:
 * comment gốc, nút mở/đóng trả lời, và từng reply (thụt vào).
 */
sealed class DiaryCommentRow {

    abstract val rowId: String

    data class CommentRow(val comment: DiaryPostComment) : DiaryCommentRow() {
        override val rowId: String = "comment:${comment.id}"
    }

    data class ReplyRow(val reply: DiaryPostComment) : DiaryCommentRow() {
        override val rowId: String = "reply:${reply.parentCommentId}:${reply.id}"
    }

    data class ToggleRepliesRow(
        val commentId: String,
        val replyCount: Int,
        val expanded: Boolean,
        val loading: Boolean,
    ) : DiaryCommentRow() {
        override val rowId: String = "toggle:$commentId"
    }
}

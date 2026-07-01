package com.example.messageapp.model

data class DiaryNavigationTarget(
    val postId: String,
    val commentId: String = "",
    val replyId: String = "",
)

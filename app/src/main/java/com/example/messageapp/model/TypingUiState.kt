package com.example.messageapp.model

data class TypingUserUi(
    val userId: String,
    val shortName: String,
    val avatarUrl: String,
)

data class TypingUiState(
    val isVisible: Boolean = false,
    val users: List<TypingUserUi> = emptyList(),
    val label: String = "",
)

package com.example.messageapp.model

data class User(
    val name: String? = "",
    val email: String? = "",
    val password: String? = "",
    val avatar: String? = "",
    val keyAuth: String? = ""
)
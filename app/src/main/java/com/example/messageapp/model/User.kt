package com.example.messageapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val name: String? = "",
    val email: String? = "",
    val avatar: String? = "",
    var keyAuth: String? = ""
) : Parcelable
package com.example.messageapp.model

data class Message(
    val message: String = "",
    val receiver: String = "",
    val sender: String = "",
    val time: String = "",
    val emotion: Map<String, Int> = mapOf(),
    val photos: ArrayList<String> = arrayListOf(),
    val singlePhoto: ArrayList<String> = arrayListOf(),
    val type: Int = 0 // 0: message, 1: photos, 2: single photo
)
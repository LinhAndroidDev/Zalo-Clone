package com.example.messageapp.model

data class GalleryItem(
    val path: String,
    val name: String,
    val isVideo: Boolean,
    val duration: String = "00:00"
)


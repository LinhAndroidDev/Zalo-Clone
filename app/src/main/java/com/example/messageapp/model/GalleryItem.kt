package com.example.messageapp.model

import android.net.Uri

data class GalleryItem(
    val path: String,
    val name: String,
    val isVideo: Boolean,
    val duration: String = "00:00",
    val contentUri: Uri,
)

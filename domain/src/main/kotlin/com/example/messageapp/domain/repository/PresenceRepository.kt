package com.example.messageapp.domain.repository

import com.example.messageapp.domain.model.UserPresence
import kotlinx.coroutines.flow.Flow

interface PresenceRepository {
    fun connect(userId: String)
    fun disconnect(userId: String)
    fun observePresence(userId: String): Flow<UserPresence>
}

interface MediaUploadRepository {
    fun uploadListPhoto(
        uriStrings: List<String>,
        roomId: List<String>,
        onProgress: (Int) -> Unit,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Throwable) -> Unit,
    )

    fun uploadAudio(
        uriString: String,
        roomId: List<String>,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
    )
}

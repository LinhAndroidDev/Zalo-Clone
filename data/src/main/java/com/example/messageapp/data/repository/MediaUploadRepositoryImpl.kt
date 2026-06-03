package com.example.messageapp.data.repository

import com.example.messageapp.data.DataContextHolder
import com.example.messageapp.data.legacy.FireBaseInstance
import com.example.messageapp.domain.repository.MediaUploadRepository
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class MediaUploadRepositoryImpl @Inject constructor() : MediaUploadRepository {

    override fun uploadListPhoto(
        uriStrings: List<String>,
        roomId: List<String>,
        onProgress: (Int) -> Unit,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        val context = DataContextHolder.appContext
        val uris = ArrayList(uriStrings.map { it.toUri() })
        FireBaseInstance.uploadListPhoto(
            context = context,
            uris = uris,
            roomId = roomId,
            process = { (_, overall) -> onProgress(overall.toInt()) },
            success = onSuccess,
            failure = onFailure,
        )
    }

    override fun uploadAudio(
        uriString: String,
        roomId: List<String>,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        FireBaseInstance.uploadAudio(
            roomId = roomId,
            uriAudio = uriString.toUri(),
            success = onSuccess,
            process = { p -> onProgress(p.toInt()) },
            failure = onFailure,
        )
    }
}

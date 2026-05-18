package com.example.messageapp.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import kotlin.math.min

object CloudinaryManager {
    // Provided by user (unsigned upload).
    private const val CLOUD_NAME = "dpdzs8jh8"
    private const val UPLOAD_PRESET = "upload_file"

    private val client = OkHttpClient()

    private fun uploadUrl(): String = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/upload"

    /**
     * @param onUploadProgress gọi trên thread OkHttp, 0f..100f theo phần body file đã ghi lên socket.
     */
    fun uploadBytes(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String? = null,
        folder: String? = null,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
        onUploadProgress: ((Float) -> Unit)? = null,
    ) {
        try {
            val mediaType =
                (mimeType ?: "application/octet-stream").toMediaTypeOrNull()
                    ?: "application/octet-stream".toMediaTypeOrNull()

            val fileBody = if (onUploadProgress != null && fileBytes.isNotEmpty()) {
                progressByteRequestBody(fileBytes, mediaType!!, onUploadProgress)
            } else {
                fileBytes.toRequestBody(mediaType!!)
            }

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                // Ensure Cloudinary detects the resource type automatically (image/audio/video).
                .addFormDataPart("resource_type", "auto")
                .apply {
                    if (!folder.isNullOrBlank()) addFormDataPart("folder", folder)
                }
                .addFormDataPart("file", fileName, fileBody)
                .build()

            val request = Request.Builder()
                .url(uploadUrl())
                .post(multipart)
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    onFailure.invoke(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        onFailure.invoke(
                            RuntimeException("Cloudinary upload failed: code=${response.code}, body=$bodyStr")
                        )
                        return
                    }

                    val json = JSONObject(bodyStr)
                    val secureUrl = json.optString("secure_url")
                    if (secureUrl.isBlank()) {
                        onFailure.invoke(RuntimeException("Cloudinary upload missing secure_url. body=$bodyStr"))
                        return
                    }
                    onUploadProgress?.invoke(100f)
                    onSuccess.invoke(secureUrl)
                }
            })
        } catch (t: Throwable) {
            onFailure.invoke(t)
        }
    }

    private fun progressByteRequestBody(
        fileBytes: ByteArray,
        mediaType: okhttp3.MediaType,
        onUploadProgress: (Float) -> Unit,
    ): RequestBody = object : RequestBody() {
        override fun contentType() = mediaType

        override fun contentLength() = fileBytes.size.toLong()

        override fun writeTo(sink: BufferedSink) {
            var offset = 0
            val total = fileBytes.size
            val segment = 16 * 1024
            var lastReported = -1f
            while (offset < total) {
                val read = min(segment, total - offset)
                sink.write(fileBytes, offset, read)
                offset += read
                val pct = offset * 100f / total
                if (pct - lastReported >= 1f || offset == total) {
                    lastReported = pct
                    onUploadProgress(pct.coerceIn(0f, 100f))
                }
            }
        }
    }

    fun uploadFileFromUri(
        context: Context,
        uri: Uri,
        fileName: String,
        mimeType: String? = null,
        folder: String? = null,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
        onUploadProgress: ((Float) -> Unit)? = null,
    ) {
        val bytes = readBytesFromUri(context.contentResolver, uri)
        uploadBytes(
            fileBytes = bytes,
            fileName = fileName,
            mimeType = mimeType,
            folder = folder,
            onSuccess = onSuccess,
            onFailure = onFailure,
            onUploadProgress = onUploadProgress,
        )
    }

    private fun readBytesFromUri(contentResolver: ContentResolver, uri: Uri): ByteArray {
        // If it's file://... we can read from filesystem without resolver permission.
        if ("file".equals(uri.scheme, ignoreCase = true) && !uri.path.isNullOrBlank()) {
            return File(requireNotNull(uri.path)).readBytes()
        }

        val inputStream: InputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open input stream for uri=$uri")
        inputStream.use { stream ->
            return stream.readBytes()
        }
    }
}

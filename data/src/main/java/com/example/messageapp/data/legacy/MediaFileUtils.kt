package com.example.messageapp.data.legacy

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File

object MediaFileUtils {

    fun Context.compressImage(uri: Uri): ByteArray {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        return outputStream.toByteArray()
    }

    fun Context.readUriBytes(uri: Uri): ByteArray {
        if ("file".equals(uri.scheme, ignoreCase = true) && !uri.path.isNullOrBlank()) {
            return File(uri.path!!).readBytes()
        }
        return contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Cannot read uri=$uri")
    }

    fun Context.isVideoUri(uri: Uri): Boolean {
        val mime = contentResolver.getType(uri) ?: return false
        return mime.startsWith("video/", ignoreCase = true)
    }
}

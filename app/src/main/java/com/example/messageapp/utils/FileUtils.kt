package com.example.messageapp.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.messageapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object FileUtils {
    private val audioBytesMemoryCache = LruCache<String, ByteArray>(20)

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

    fun isLikelyVideoUrl(url: String): Boolean {
        if (url.contains("/video/upload", ignoreCase = true)) return true
        val lower = url.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".webm") ||
            lower.endsWith(".mov") || lower.endsWith(".3gp")
    }

    fun Context.loadImg(url: String, cir: ImageView, imgDefault: Int = R.mipmap.ic_launcher) {
        Glide.with(this)
            .load(url)
            .placeholder(imgDefault)
            .error(imgDefault)
            .into(cir)
    }

    suspend fun downloadAndSaveImage(context: Context, imageUrl: String) {
        try {
            // Tải ảnh từ URL về dạng Bitmap
            val bitmap = downloadImageFromUrl(imageUrl)
            bitmap?.let {
                saveImageToGallery(context, it)
                Toast.makeText(context, "Đã lưu ảnh", Toast.LENGTH_SHORT).show()
            } ?: Log.e("DownloadImage", "Không thể tải ảnh")
        } catch (e: Exception) {
            Toast.makeText(context, "DownloadImage: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun downloadImageFromUrl(imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream: InputStream = connection.inputStream
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                Log.e("DownloadImage", "Lỗi khi tải ảnh: ${e.message}")
                null
            }
        }
    }

    private fun saveImageToGallery(context: Context, bitmap: Bitmap) {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }
    }

    fun fileToByteArray(filePath: String): ByteArray? {
        return try {
            val file = File(filePath)
            file.readBytes()
        } catch (e: IOException) {
            Log.e("FileConversion", "Lỗi khi chuyển file sang ByteArray: ${e.message}")
            null
        }
    }

    suspend fun getOrDownloadAudioFile(context: Context, audioUrl: String): File? {
        if (audioUrl.isBlank()) return null
        val cacheFile = getAudioCacheFile(context, audioUrl)
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return cacheFile
        }

        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(audioUrl).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null

                val bytes = response.body?.bytes() ?: return@withContext null
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeBytes(bytes)
                audioBytesMemoryCache.put(audioUrl, bytes)
                cacheFile
            } catch (e: IOException) {
                Log.e("AudioCache", "Cannot cache audio: ${e.message}")
                null
            }
        }
    }

    suspend fun downloadAudioFile(context: Context, fileUrl: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("Download", "Server returned HTTP ${connection.responseCode}")
                    return@withContext null
                }

                // Tạo file trong thư mục internal
                val audioDir = File(context.filesDir, "audios")
                if (!audioDir.exists()) {
                    audioDir.mkdirs()
                }

                val outputFile = File(audioDir, getFileNameFromUrl(fileUrl))

                connection.inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }

                Log.d("Download", "Saved to: ${outputFile.absolutePath}")
                return@withContext outputFile
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    private fun getAudioCacheFile(context: Context, audioUrl: String): File {
        val cacheDir = File(context.cacheDir, "audio_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val fileName = "${audioUrl.md5()}.mp3"
        return File(cacheDir, fileName)
    }

    private fun String.md5(): String {
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
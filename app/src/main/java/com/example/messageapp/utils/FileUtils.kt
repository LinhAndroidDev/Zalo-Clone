package com.example.messageapp.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object FileUtils {
    fun Context.compressImage(uri: Uri): ByteArray {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        return outputStream.toByteArray()
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

    fun convertMp3UrlToByteArray(mp3Url: String): ByteArray? {
        return try {
            val client = OkHttpClient()
            val request = Request.Builder().url(mp3Url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
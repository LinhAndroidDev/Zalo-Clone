package com.example.messageapp.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import com.example.messageapp.model.GalleryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GalleryUtils {
    
    suspend fun getGalleryItems(context: Context): List<GalleryItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<GalleryItem>()
        
        // Get images
        val imageItems = getImages(context.contentResolver)
        items.addAll(imageItems)
        
        // Get videos
        val videoItems = getVideos(context.contentResolver)
        items.addAll(videoItems)
        
        // Sort by date (newest first)
        items.sortedByDescending { 
            try {
                java.io.File(it.path).lastModified()
            } catch (e: Exception) {
                0L
            }
        }
    }
    
    private fun getImages(contentResolver: ContentResolver): List<GalleryItem> {
        val images = mutableListOf<GalleryItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )
        
        val selection = "${MediaStore.Images.Media.DATA} IS NOT NULL"
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val path = cursor.getString(dataColumn)
                
                if (path != null) {
                    images.add(GalleryItem(path, name, false))
                }
            }
        }
        
        return images.take(50) // Limit to 50 images
    }
    
    private fun getVideos(contentResolver: ContentResolver): List<GalleryItem> {
        val videos = mutableListOf<GalleryItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED
        )
        
        val selection = "${MediaStore.Video.Media.DATA} IS NOT NULL"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        
        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val path = cursor.getString(dataColumn)
                val duration = cursor.getLong(durationColumn)
                
                if (path != null) {
                    val durationText = formatDuration(duration)
                    videos.add(GalleryItem(path, name, true, durationText))
                }
            }
        }
        
        return videos.take(20) // Limit to 20 videos
    }
    
    @SuppressLint("DefaultLocale")
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}


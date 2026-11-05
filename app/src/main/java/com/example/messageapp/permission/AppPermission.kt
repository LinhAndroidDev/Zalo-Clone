package com.example.messageapp.permission

import android.Manifest
import android.os.Build
import androidx.annotation.StringRes
import com.example.messageapp.R

/**
 * Nhóm quyền theo TÍNH NĂNG của app. Mọi nơi xin quyền chỉ dùng enum này,
 * không tự viết chuỗi Manifest.permission rải rác.
 *
 * @param requireAll true: phải được cấp tất cả quyền trong nhóm; false: chỉ cần một quyền.
 */
enum class AppPermission(
    val permissions: Array<String>,
    @StringRes val deniedMessageRes: Int,
    val requireAll: Boolean = true,
) {
    CAMERA(
        arrayOf(Manifest.permission.CAMERA),
        R.string.permission_camera_denied,
    ),
    MICROPHONE(
        arrayOf(Manifest.permission.RECORD_AUDIO),
        R.string.permission_microphone_denied,
    ),
    /** Gọi thoại. */
    VOICE_CALL(
        arrayOf(Manifest.permission.RECORD_AUDIO),
        R.string.permission_voice_call_denied,
    ),
    /** Gọi video: camera + micro. */
    VIDEO_CALL(
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
        R.string.permission_video_call_denied,
    ),
    /** Thư viện ảnh/video (đã tách theo phiên bản Android). */
    MEDIA_LIBRARY(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        },
        R.string.permission_media_denied,
        requireAll = false,
    ),
    /** Thông báo: chỉ cần xin từ Android 13. */
    NOTIFICATIONS(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        },
        R.string.permission_notifications_denied,
    ),
}

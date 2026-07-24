package com.example.messageapp.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

object StoryFirestore {
    const val COLLECTION = "stories"
    const val SUB_VIEWS = "views"

    const val FIELD_AUTHOR_ID = "authorId"
    const val FIELD_AUTHOR_NAME = "authorName"
    const val FIELD_AUTHOR_AVATAR = "authorAvatarUrl"
    const val FIELD_MEDIA_URL = "mediaUrl"
    const val FIELD_MEDIA_TYPE = "mediaType"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_EXPIRES_AT = "expiresAt"
    const val FIELD_PRIVACY = "privacy"
    const val FIELD_VISIBLE_TO = "visibleToUserIds"
    const val FIELD_MUSIC_TRACK_ID = "musicTrackId"
    const val FIELD_MUSIC_NAME = "musicName"
    const val FIELD_MUSIC_ARTIST = "musicArtist"
    const val FIELD_MUSIC_AUDIO_URL = "musicAudioUrl"
    const val FIELD_MUSIC_IMAGE_URL = "musicImageUrl"
    const val FIELD_MUSIC_STICKER_X = "musicStickerX"
    const val FIELD_MUSIC_STICKER_Y = "musicStickerY"
    const val FIELD_MEDIA_SCALE = "mediaScale"
    const val FIELD_MEDIA_ROTATION = "mediaRotation"
    const val FIELD_MEDIA_TRANSLATION_X = "mediaTranslationX"
    const val FIELD_MEDIA_TRANSLATION_Y = "mediaTranslationY"
    const val FIELD_VIEWED_AT = "viewedAt"

    const val MEDIA_IMAGE = "image"
    const val MEDIA_VIDEO = "video"
    const val PRIVACY_EVERYONE = "everyone"
    const val PRIVACY_FRIENDS = "friends"
    const val PRIVACY_CUSTOM = "custom"

    fun fromDocument(doc: DocumentSnapshot, viewedByMe: Boolean = false): Story? {
        val data = doc.data ?: return null
        val authorId = data[FIELD_AUTHOR_ID]?.toString().orEmpty()
        if (authorId.isBlank()) return null
        val mediaTypeRaw = data[FIELD_MEDIA_TYPE]?.toString().orEmpty()
        val mediaType = if (mediaTypeRaw == MEDIA_VIDEO) MEDIA_VIDEO else MEDIA_IMAGE
        @Suppress("UNCHECKED_CAST")
        val visibleToUserIds = (data[FIELD_VISIBLE_TO] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
        return Story(
            id = doc.id,
            authorId = authorId,
            authorName = data[FIELD_AUTHOR_NAME]?.toString().orEmpty(),
            authorAvatarUrl = data[FIELD_AUTHOR_AVATAR]?.toString().orEmpty(),
            mediaUrl = data[FIELD_MEDIA_URL]?.toString().orEmpty(),
            mediaType = mediaType,
            createdAtMillis = parseMillis(data[FIELD_CREATED_AT]),
            expiresAtMillis = parseMillis(data[FIELD_EXPIRES_AT]),
            privacy = data[FIELD_PRIVACY]?.toString().orEmpty(),
            visibleToUserIds = visibleToUserIds,
            musicTrackId = data[FIELD_MUSIC_TRACK_ID]?.toString().orEmpty(),
            musicName = data[FIELD_MUSIC_NAME]?.toString().orEmpty(),
            musicArtist = data[FIELD_MUSIC_ARTIST]?.toString().orEmpty(),
            musicAudioUrl = data[FIELD_MUSIC_AUDIO_URL]?.toString().orEmpty(),
            musicImageUrl = data[FIELD_MUSIC_IMAGE_URL]?.toString().orEmpty(),
            musicStickerX = parseNormalizedFloat(data[FIELD_MUSIC_STICKER_X], 0.5f),
            musicStickerY = parseNormalizedFloat(data[FIELD_MUSIC_STICKER_Y], 0.5f),
            mediaScale = parseFloat(data[FIELD_MEDIA_SCALE], 1f),
            mediaRotation = parseFloat(data[FIELD_MEDIA_ROTATION], 0f),
            mediaTranslationX = parseFloat(data[FIELD_MEDIA_TRANSLATION_X], 0f),
            mediaTranslationY = parseFloat(data[FIELD_MEDIA_TRANSLATION_Y], 0f),
            viewedByMe = viewedByMe,
        )
    }

    private fun parseNormalizedFloat(value: Any?, default: Float): Float = when (value) {
        is Number -> value.toFloat().coerceIn(0f, 1f)
        else -> default
    }

    private fun parseFloat(value: Any?, default: Float): Float = when (value) {
        is Number -> value.toFloat()
        else -> default
    }

    private fun parseMillis(value: Any?): Long = when (value) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> System.currentTimeMillis()
    }
}

data class Story(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val mediaUrl: String,
    val mediaType: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val privacy: String,
    val visibleToUserIds: List<String> = emptyList(),
    val musicTrackId: String = "",
    val musicName: String = "",
    val musicArtist: String = "",
    val musicAudioUrl: String = "",
    val musicImageUrl: String = "",
    val musicStickerX: Float = 0.5f,
    val musicStickerY: Float = 0.5f,
    val mediaScale: Float = 1f,
    val mediaRotation: Float = 0f,
    val mediaTranslationX: Float = 0f,
    val mediaTranslationY: Float = 0f,
    val viewedByMe: Boolean = false,
)

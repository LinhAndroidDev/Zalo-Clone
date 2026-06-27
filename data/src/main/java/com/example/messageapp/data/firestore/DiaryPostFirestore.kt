package com.example.messageapp.data.firestore

import com.example.messageapp.domain.model.EmotionType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Maps Firestore document fields for collection [posts].
 */
object DiaryPostFirestore {
    const val COLLECTION = "posts"
    const val SUB_LIKES = "likes"
    const val SUB_REACTIONS = "reactions"
    const val SUB_COMMENTS = "comments"
    const val SUB_COMMENT_LIKES = "likes"
    const val SUB_REPLIES = "replies"

    const val FIELD_AUTHOR_ID = "authorId"
    const val FIELD_AUTHOR_NAME = "authorName"
    const val FIELD_AUTHOR_AVATAR = "authorAvatarUrl"
    const val FIELD_CONTENT = "content"
    const val FIELD_IMAGE_URLS = "imageUrls"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_UPDATED_AT = "updatedAt"
    const val FIELD_LIKE_COUNT = "likeCount"
    const val FIELD_COMMENT_COUNT = "commentCount"
    const val FIELD_LINK_PREVIEW = "linkPreview"
    const val FIELD_EMOTION_SUMMARY = "emotionSummary"
    const val REACTION_FIELD_TYPE = "type"

    const val LINK_FIELD_URL = "url"
    const val LINK_FIELD_TITLE = "title"
    const val LINK_FIELD_DESCRIPTION = "description"
    const val LINK_FIELD_IMAGE_URL = "imageUrl"

    const val COMMENT_FIELD_AUTHOR_ID = "authorId"
    const val COMMENT_FIELD_AUTHOR_NAME = "authorName"
    const val COMMENT_FIELD_AUTHOR_AVATAR = "authorAvatarUrl"
    const val COMMENT_FIELD_TEXT = "text"
    const val COMMENT_FIELD_CREATED_AT = "createdAt"
    const val COMMENT_FIELD_LIKE_COUNT = "likeCount"
    const val COMMENT_FIELD_REPLY_COUNT = "replyCount"
    const val COMMENT_FIELD_PARENT_ID = "parentCommentId"
    const val COMMENT_FIELD_MENTIONED_ID = "mentionedUserId"
    const val COMMENT_FIELD_MENTIONED_NAME = "mentionedName"

    fun fromDocument(
        doc: DocumentSnapshot,
        likedByMe: Boolean = false,
        myReactionType: EmotionType? = null,
    ): DiaryPost? {
        val id = doc.id
        val data = doc.data ?: return null
        val authorId = data[FIELD_AUTHOR_ID]?.toString() ?: return null
        val authorName = data[FIELD_AUTHOR_NAME]?.toString().orEmpty()
        val authorAvatarUrl = data[FIELD_AUTHOR_AVATAR]?.toString().orEmpty()
        val content = data[FIELD_CONTENT]?.toString().orEmpty()
        @Suppress("UNCHECKED_CAST")
        val imageUrls = (data[FIELD_IMAGE_URLS] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val linkMap = data[FIELD_LINK_PREVIEW] as? Map<*, *>
        val linkPreview = linkMap?.let { m ->
            val u = m[LINK_FIELD_URL]?.toString()?.trim().orEmpty()
            if (u.isEmpty()) null
            else {
                val img = m[LINK_FIELD_IMAGE_URL]?.toString()?.trim().orEmpty()
                DiaryLinkPreview(
                    url = u,
                    title = m[LINK_FIELD_TITLE]?.toString().orEmpty(),
                    description = m[LINK_FIELD_DESCRIPTION]?.toString().orEmpty(),
                    imageUrl = img.ifBlank { null }
                )
            }
        }
        val createdAtMillis = when (val t = data[FIELD_CREATED_AT]) {
            is Timestamp -> t.toDate().time
            is Number -> t.toLong()
            else -> System.currentTimeMillis()
        }
        val likeCount = (data[FIELD_LIKE_COUNT] as? Number)?.toInt() ?: 0
        val commentCount = (data[FIELD_COMMENT_COUNT] as? Number)?.toInt() ?: 0
        val emotionCounts = parseEmotionSummary(data[FIELD_EMOTION_SUMMARY])
        val reaction = myReactionType ?: if (likedByMe) EmotionType.LIKE else null
        return DiaryPost(
            id = id,
            authorUserId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            content = content,
            imageUris = imageUrls,
            linkPreview = linkPreview,
            createdAtMillis = createdAtMillis,
            likeCount = likeCount,
            commentCount = commentCount,
            likedByMe = reaction != null,
            myReactionType = reaction?.name.orEmpty(),
            emotionCounts = emotionCounts,
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun parseEmotionSummary(raw: Any?): Map<String, Int> {
        val map = raw as? Map<*, *> ?: return emptyMap()
        return map.mapNotNull { (k, v) ->
            val key = k?.toString()?.trim().orEmpty()
            val count = (v as? Number)?.toInt() ?: 0
            if (key.isBlank() || count <= 0) null else key to count
        }.toMap()
    }

    fun commentFromDocument(postId: String, doc: DocumentSnapshot): DiaryPostComment? {
        val data = doc.data ?: return null
        val createdAtMillis = when (val t = data[COMMENT_FIELD_CREATED_AT]) {
            is Timestamp -> t.toDate().time
            is Number -> t.toLong()
            else -> System.currentTimeMillis()
        }
        return DiaryPostComment(
            id = doc.id,
            postId = postId,
            authorId = data[COMMENT_FIELD_AUTHOR_ID]?.toString().orEmpty(),
            authorName = data[COMMENT_FIELD_AUTHOR_NAME]?.toString().orEmpty(),
            authorAvatarUrl = data[COMMENT_FIELD_AUTHOR_AVATAR]?.toString().orEmpty(),
            text = data[COMMENT_FIELD_TEXT]?.toString().orEmpty(),
            createdAtMillis = createdAtMillis,
            likeCount = (data[COMMENT_FIELD_LIKE_COUNT] as? Number)?.toInt() ?: 0,
            replyCount = (data[COMMENT_FIELD_REPLY_COUNT] as? Number)?.toInt() ?: 0,
            parentCommentId = "",
        )
    }

    /** Reply nằm trong posts/{postId}/comments/{parentCommentId}/replies/{replyId}. */
    fun replyFromDocument(
        postId: String,
        parentCommentId: String,
        doc: DocumentSnapshot,
    ): DiaryPostComment? {
        val data = doc.data ?: return null
        val createdAtMillis = when (val t = data[COMMENT_FIELD_CREATED_AT]) {
            is Timestamp -> t.toDate().time
            is Number -> t.toLong()
            else -> System.currentTimeMillis()
        }
        return DiaryPostComment(
            id = doc.id,
            postId = postId,
            authorId = data[COMMENT_FIELD_AUTHOR_ID]?.toString().orEmpty(),
            authorName = data[COMMENT_FIELD_AUTHOR_NAME]?.toString().orEmpty(),
            authorAvatarUrl = data[COMMENT_FIELD_AUTHOR_AVATAR]?.toString().orEmpty(),
            text = data[COMMENT_FIELD_TEXT]?.toString().orEmpty(),
            createdAtMillis = createdAtMillis,
            likeCount = (data[COMMENT_FIELD_LIKE_COUNT] as? Number)?.toInt() ?: 0,
            replyCount = 0,
            parentCommentId = parentCommentId,
            mentionedUserId = data[COMMENT_FIELD_MENTIONED_ID]?.toString().orEmpty(),
            mentionedName = data[COMMENT_FIELD_MENTIONED_NAME]?.toString().orEmpty(),
        )
    }
}

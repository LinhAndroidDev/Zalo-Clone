package com.example.messageapp.mapper

import com.example.messageapp.data.firestore.DiaryLinkPreview as FsDiaryLinkPreview
import com.example.messageapp.data.firestore.DiaryPost as FsDiaryPost
import com.example.messageapp.data.firestore.DiaryPostComment as FsDiaryPostComment
import com.example.messageapp.domain.model.DiaryLinkPreview as DomainDiaryLinkPreview
import com.example.messageapp.domain.model.DiaryPost as DomainDiaryPost
import com.example.messageapp.domain.model.DiaryPostComment as DomainDiaryPostComment
import com.example.messageapp.model.DiaryLinkPreview
import com.example.messageapp.model.DiaryPost
import com.example.messageapp.model.DiaryPostComment
import com.example.messageapp.model.EmotionType

object DiaryUiMapper {

    fun toUi(preview: DomainDiaryLinkPreview): DiaryLinkPreview = DiaryLinkPreview(
        url = preview.url,
        title = preview.title,
        description = preview.description,
        imageUrl = preview.imageUrl,
    )

    fun toDomain(preview: DiaryLinkPreview?): DomainDiaryLinkPreview? = preview?.let {
        DomainDiaryLinkPreview(
            url = it.url,
            title = it.title,
            description = it.description,
            imageUrl = it.imageUrl,
        )
    }

    fun toUi(preview: FsDiaryLinkPreview): DiaryLinkPreview = toUi(
        DomainDiaryLinkPreview(
            url = preview.url,
            title = preview.title,
            description = preview.description,
            imageUrl = preview.imageUrl,
        ),
    )

    fun toFirestore(preview: DiaryLinkPreview): FsDiaryLinkPreview = FsDiaryLinkPreview(
        url = preview.url,
        title = preview.title,
        description = preview.description,
        imageUrl = preview.imageUrl,
    )

    fun toUi(comment: DomainDiaryPostComment): DiaryPostComment = DiaryPostComment(
        id = comment.id,
        postId = comment.postId,
        authorId = comment.authorId,
        authorName = comment.authorName,
        authorAvatarUrl = comment.authorAvatarUrl,
        text = comment.text,
        createdAtMillis = comment.createdAtMillis,
        likeCount = comment.likeCount,
        likedByMe = comment.likedByMe,
        replyCount = comment.replyCount,
        parentCommentId = comment.parentCommentId,
        mentionedUserId = comment.mentionedUserId,
        mentionedName = comment.mentionedName,
    )

    fun toUi(comment: FsDiaryPostComment): DiaryPostComment = DiaryPostComment(
        id = comment.id,
        postId = comment.postId,
        authorId = comment.authorId,
        authorName = comment.authorName,
        authorAvatarUrl = comment.authorAvatarUrl,
        text = comment.text,
        createdAtMillis = comment.createdAtMillis,
        likeCount = comment.likeCount,
        likedByMe = comment.likedByMe,
        replyCount = comment.replyCount,
        parentCommentId = comment.parentCommentId,
        mentionedUserId = comment.mentionedUserId,
        mentionedName = comment.mentionedName,
    )

    fun toUi(post: DomainDiaryPost): DiaryPost = DiaryPost(
        id = post.id,
        authorUserId = post.authorUserId,
        authorName = post.authorName,
        authorAvatarUrl = post.authorAvatarUrl,
        content = post.content,
        imageUris = post.imageUris,
        linkPreview = post.linkPreview?.let { toUi(it) },
        createdAtMillis = post.createdAtMillis,
        likeCount = post.likeCount,
        commentCount = post.commentCount,
        likedByMe = post.likedByMe,
        myReactionType = post.myReactionType?.let { EmotionType.valueOf(it.name) },
        emotionCounts = post.emotionCounts.mapKeys { EmotionType.valueOf(it.key.name) },
    )

    fun toUi(post: FsDiaryPost): DiaryPost = DiaryPost(
        id = post.id,
        authorUserId = post.authorUserId,
        authorName = post.authorName,
        authorAvatarUrl = post.authorAvatarUrl,
        content = post.content,
        imageUris = post.imageUris,
        linkPreview = post.linkPreview?.let { toUi(it) },
        createdAtMillis = post.createdAtMillis,
        likeCount = post.likeCount,
        commentCount = post.commentCount,
        likedByMe = post.likedByMe,
        myReactionType = post.myReactionType.takeIf { it.isNotBlank() }
            ?.let { runCatching { EmotionType.valueOf(it) }.getOrNull() },
        emotionCounts = post.emotionCounts.mapNotNull { (k, v) ->
            runCatching { EmotionType.valueOf(k) to v }.getOrNull()
        }.toMap(),
    )

    fun toUiPosts(posts: List<DomainDiaryPost>): List<DiaryPost> = posts.map { toUi(it) }

    fun toUiPostsFromFirestore(posts: List<FsDiaryPost>): List<DiaryPost> = posts.map { toUi(it) }

    fun toDomain(post: DiaryPost): DomainDiaryPost = DomainDiaryPost(
        id = post.id,
        authorUserId = post.authorUserId,
        authorName = post.authorName,
        authorAvatarUrl = post.authorAvatarUrl,
        content = post.content,
        imageUris = post.imageUris,
        linkPreview = toDomain(post.linkPreview),
        createdAtMillis = post.createdAtMillis,
        likeCount = post.likeCount,
        commentCount = post.commentCount,
        likedByMe = post.likedByMe,
        myReactionType = post.myReactionType?.let {
            com.example.messageapp.domain.model.EmotionType.valueOf(it.name)
        },
        emotionCounts = post.emotionCounts.mapKeys {
            com.example.messageapp.domain.model.EmotionType.valueOf(it.key.name)
        },
    )
}

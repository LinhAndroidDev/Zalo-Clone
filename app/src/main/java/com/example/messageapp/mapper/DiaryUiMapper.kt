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
    )

    fun toUi(comment: FsDiaryPostComment): DiaryPostComment = DiaryPostComment(
        id = comment.id,
        postId = comment.postId,
        authorId = comment.authorId,
        authorName = comment.authorName,
        authorAvatarUrl = comment.authorAvatarUrl,
        text = comment.text,
        createdAtMillis = comment.createdAtMillis,
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
    )
}

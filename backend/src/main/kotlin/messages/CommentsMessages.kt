package com.hexagonkt.realworld.messages

import com.hexagonkt.core.requirePath
import com.hexagonkt.realworld.domain.model.Comment
import com.hexagonkt.realworld.domain.model.User

data class CommentRequest(val body: String) {

    constructor(data: Map<*, *>) : this(
        data.requirePath<String>(CommentRequest::body),
    )
}

data class CommentResponse(
    val id: Int,
    val createdAt: String,
    val updatedAt: String,
    val body: String,
    val author: AuthorResponse
) {
    constructor(data: Map<*, *>) : this(
        data.requirePath(CommentResponse::id),
        data.requirePath(CommentResponse::createdAt),
        data.requirePath(CommentResponse::updatedAt),
        data.requirePath(CommentResponse::body),
        AuthorResponse(data.requirePath(CommentResponse::author)),
    )

    constructor(comment: Comment, author: User, user: User?): this(
        id = comment.id,
        createdAt = comment.createdAt.toUtc(),
        updatedAt = comment.updatedAt.toUtc(),
        body = comment.body,
        author = AuthorResponse(
            username = author.username,
            bio = author.bio ?: "",
            image = author.image?.toString() ?: "",
            following = user?.following?.contains(author.username) ?: false
        )
    )
}

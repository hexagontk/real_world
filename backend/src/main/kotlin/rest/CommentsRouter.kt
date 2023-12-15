package com.hexagonkt.realworld.rest

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.core.require
import com.hexagonkt.core.requirePath
import com.hexagonkt.http.handlers.path
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.realworld.*
import com.hexagonkt.rest.bodyMap
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.realworld.domain.model.Comment
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.realworld.rest.messages.CommentRequest
import com.hexagonkt.realworld.rest.messages.CommentResponse
import com.hexagonkt.realworld.rest.messages.OkResponse
import com.hexagonkt.serialization.serialize
import com.hexagonkt.store.Store

internal data class CommentsRouter(
    private val jwt: Jwt,
    private val users: Store<User, String>,
    private val articles: Store<Article, String>,
    private val contentType: ContentType,
) {
    val commentsRouter = path {
        post {
            val principal = jwt.parsePrincipal(this) ?: return@post unauthorized("Unauthorized")
            val subject = principal.subject
            val slug = pathParameters.require(Article::slug.name)
            val article = articles.findOne(slug) ?: return@post notFound("$slug article not found")
            val author = users.findOne(article.author)
                ?: return@post notFound("${article.author} user not found")
            val user = users.findOne(subject) ?: return@post notFound("$subject user not found")
            val commentRequest =
                CommentRequest(request.bodyMap().requirePath<Map<String, Any>>("comment"))
            val comment = Comment(
                id = (article.comments.maxOfOrNull { it.id } ?: 0) + 1,
                author = subject,
                body = commentRequest.body
            )

            val updated = articles.replaceOne(article.copy(comments = article.comments + comment))

            if (!updated)
                return@post internalServerError("Not updated")

            val content = mapOf("comment" to CommentResponse(comment, author, user))

            ok(content.serialize(APPLICATION_JSON), contentType = contentType)
        }

        get {
            val principal = jwt.parsePrincipal(this)
            val subject = principal?.subject
            val slug = pathParameters.require(Article::slug.name)
            val article = articles.findOne(slug) ?: return@get notFound("$slug article not found")
            val author = users.findOne(article.author)
                ?: return@get notFound("${article.author} user not found")
            val user =
                if (subject != null) users.findOne(subject)
                    ?: return@get notFound("$subject user not found")
                else null

            val content = article.comments.map { CommentResponse(it, author, user) }

            ok(mapOf("comments" to content).serialize(APPLICATION_JSON), contentType = contentType)
        }

        delete("/{id}") {
            jwt.parsePrincipal(this) ?: return@delete unauthorized("Unauthorized")
            val slug = pathParameters.require(Article::slug.name)
            val article =
                articles.findOne(slug) ?: return@delete notFound("$slug article not found")
            val id = pathParameters.require(Comment::id.name).toInt()
            val newArticle = article.copy(comments = article.comments.filter { it.id != id })
            val updated = articles.replaceOne(newArticle)

            if (!updated)
                return@delete internalServerError("Not updated")

            ok(OkResponse("$id deleted").serialize(APPLICATION_JSON), contentType = contentType)
        }
    }
}

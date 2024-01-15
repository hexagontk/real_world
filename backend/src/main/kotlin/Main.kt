package com.hexagonkt.realworld

import com.hexagonkt.core.*
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.realworld.domain.model.Comment
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.store.Store
import com.hexagonkt.store.mongodb.MongoDbStore
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import java.net.URI

val application by lazy {
    val settings = Settings()
    val jwt = createJwt(settings)
    val userStore = createUserStore(settings)
    val articleStore = createArticleStore(settings)

    Application(settings, jwt, userStore, articleStore)
}

internal fun main() {
    application.start()
}

internal fun createJwt(settings: Settings): Jwt =
    Jwt(urlOf(settings.keyStoreResource), settings.keyStorePassword, settings.keyPairAlias)

private fun createUserStore(settings: Settings): Store<User, String> {
    val userStore = MongoDbStore(
        type = User::class,
        key = User::username,
        database = MongoDbStore.database(settings.mongodbUrl),
        encoder = {
            fieldsMapOfNotNull(
                User::username to it.username,
                User::email to it.email,
                User::password to it.password,
                User::bio to it.bio,
                User::image to it.image,
                User::following to it.following,
            )
        },
        decoder = {
            User(
                username = it.requireString(User::username),
                email = it.requireString(User::email),
                password = it.requireString(User::password),
                bio = it.getString(User::bio),
                image = it.getString(User::image)?.let(::URI),
                following = it.getStringsOrEmpty(User::following).toSet(),
            )
        },
    )

    val indexField = User::email.name
    val indexOptions = IndexOptions().unique(true).background(true).name(indexField)
    userStore.collection.createIndex(Indexes.ascending(indexField), indexOptions)

    return userStore
}

private fun createArticleStore(settings: Settings): Store<Article, String> {
    val articleStore = MongoDbStore(
        type = Article::class,
        key = Article::slug,
        database = MongoDbStore.database(settings.mongodbUrl),
        encoder = {
            fieldsMapOfNotNull(
                Article::slug to it.slug,
                Article::author to it.author,
                Article::title to it.title,
                Article::description to it.description,
                Article::body to it.body,
                Article::tagList to it.tagList,
                Article::createdAt to it.createdAt,
                Article::updatedAt to it.updatedAt,
                Article::favoritedBy to it.favoritedBy,
                Article::comments to it.comments.map { m ->
                    fieldsMapOfNotNull(
                        Comment::id to m.id,
                        Comment::author to m.author,
                        Comment::body to m.body,
                        Comment::createdAt to m.createdAt,
                        Comment::updatedAt to m.updatedAt,
                    )
                },
            )
        },
        decoder = {
            Article(
                slug = it.requireString(Article::slug),
                author = it.requireString(Article::author),
                title = it.requireString(Article::title),
                description = it.requireString(Article::description),
                body = it.requireString(Article::body),
                tagList = it.getStringsOrEmpty(Article::tagList).let(::LinkedHashSet),
                createdAt = it.requireKey(Comment::createdAt),
                updatedAt = it.requireKey(Comment::updatedAt),
                favoritedBy = it.getStringsOrEmpty(Article::favoritedBy).toSet(),
                comments = it.getMapsOrEmpty(Article::comments).map { m ->
                    Comment(
                        id = m.requireInt(Comment::id),
                        author = m.requireString(Comment::author),
                        body = m.requireString(Comment::body),
                        createdAt = m.requireKey(Comment::createdAt),
                        updatedAt = m.requireKey(Comment::updatedAt),
                    )
                },
            )
        },
    )

    val indexField = Article::author.name
    val indexOptions = IndexOptions().unique(false).background(true).name(indexField)
    articleStore.collection.createIndex(Indexes.ascending(indexField), indexOptions)

    return articleStore
}

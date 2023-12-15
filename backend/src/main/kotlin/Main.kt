package com.hexagonkt.realworld

import com.hexagonkt.converters.ConvertersManager
import com.hexagonkt.converters.convert
import com.hexagonkt.core.*
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.realworld.domain.model.Comment
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.store.Store
import com.hexagonkt.store.mongodb.MongoDbStore
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes

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
    val userStore = MongoDbStore(User::class, User::username, settings.mongodbUrl)
    val indexField = User::email.name
    val indexOptions = IndexOptions().unique(true).background(true).name(indexField)
    userStore.collection.createIndex(Indexes.ascending(indexField), indexOptions)

    ConvertersManager.register(User::class to Map::class) {
        fieldsMapOfNotNull(
            User::username to it.username,
            User::email to it.email,
            User::password to it.password,
            User::bio to it.bio,
            User::image to it.image,
            User::following to it.following,
        )
    }
    ConvertersManager.register(Map::class to User::class) {
        User(
            username = it.requireString(User::username),
            email = it.requireString(User::email),
            password = it.requireString(User::password),
            bio = it.getString(User::bio),
            image = it.getString(User::image)?.let(::urlOf),
            following = it.getStringsOrEmpty(User::following).toSet(),
        )
    }

    return userStore
}

private fun createArticleStore(settings: Settings): Store<Article, String> {
    val articleStore = MongoDbStore(Article::class, Article::slug, settings.mongodbUrl)
    val indexField = Article::author.name
    val indexOptions = IndexOptions().unique(false).background(true).name(indexField)
    articleStore.collection.createIndex(Indexes.ascending(indexField), indexOptions)

    ConvertersManager.register(Comment::class to Map::class) {
        fieldsMapOfNotNull(
            Comment::id to it.id,
            Comment::author to it.author,
            Comment::body to it.body,
            Comment::createdAt to it.createdAt,
            Comment::updatedAt to it.updatedAt,
        )
    }
    ConvertersManager.register(Map::class to Comment::class) {
        Comment(
            id = it.requireInt(Comment::id),
            author = it.requireString(Comment::author),
            body = it.requireString(Comment::body),
            createdAt = it.requireKey(Comment::createdAt),
            updatedAt = it.requireKey(Comment::updatedAt),
        )
    }
    ConvertersManager.register(Article::class to Map::class) {
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
            Article::comments to it.comments.map { m -> m.convert(Map::class) },
        )
    }
    ConvertersManager.register(Map::class to Article::class) {
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
            comments = it.getMapsOrEmpty(Article::comments).map { m -> m.convert(Comment::class) },
        )
    }

    return articleStore
}

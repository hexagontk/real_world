package com.hexagonkt.realworld.rest

import com.auth0.jwt.interfaces.DecodedJWT
import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.core.require
import com.hexagonkt.core.requirePath
import com.hexagonkt.core.withZone
import com.hexagonkt.http.handlers.HttpContext
import com.hexagonkt.http.handlers.HttpHandler
import com.hexagonkt.http.handlers.path
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.realworld.Jwt
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.realworld.rest.messages.*
import com.hexagonkt.rest.bodyMap
import com.hexagonkt.serialization.serialize
import com.hexagonkt.store.Store
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

internal data class ArticlesRouter(
    private val jwt: Jwt,
    private val users: Store<User, String>,
    private val articles: Store<Article, String>,
    private val contentType: ContentType,
    private val authenticator: HttpHandler,
    private val commentsRouter: HttpHandler,
) {
    val articlesRouter by lazy {
        path {
            get("/feed") { getFeed(jwt, users, articles) }

            path("/(?!feed)(?<slug>[^/]+?)") {

                path("/favorite") {
                    use(authenticator)
                    post { favoriteArticle(users, articles, true) }
                    delete { favoriteArticle(users, articles, false) }
                }

                path("/comments", commentsRouter)

                delete { deleteArticle(jwt, articles) }
                put { updateArticle(jwt, articles) }
                get { getArticle(jwt, users, articles) }
            }

            post { createArticle(jwt, articles) }
            get { findArticles(jwt, users, articles) }
        }
    }

    fun HttpContext.findArticles(
        jwt: Jwt, users: Store<User, String>, articles: Store<Article, String>): HttpContext {

        val principal = jwt.parsePrincipal(this)
        val subject = principal?.subject
        val filter = queryParameters
            .mapKeys {
                when (it.key) {
                    "tag" -> Article::tagList.name
                    "favorited" -> Article::favoritedBy.name
                    else -> it.key
                }
            }
            .mapValues { it.value.value }

        val foundArticles = searchArticles(users, articles, subject, filter)
        return ok(foundArticles.serialize(APPLICATION_JSON), contentType = contentType)
    }

    private fun HttpContext.createArticle(
        jwt: Jwt, articles: Store<Article, String>
    ): HttpContext {
        val principal = jwt.parsePrincipal(this) ?: return unauthorized("Unauthorized")
        val bodyArticle = ArticleRequest(request.bodyMap().requirePath("article"))
        val article = Article(
            slug = bodyArticle.title.toSlug(),
            author = principal.subject,
            title = bodyArticle.title,
            description = bodyArticle.description,
            body = bodyArticle.body,
            tagList = bodyArticle.tagList
        )

        articles.insertOne(article)

        val articleCreationResponseRoot = ArticleCreationResponseRoot(article, principal.subject)
        return ok(articleCreationResponseRoot.serialize(APPLICATION_JSON), contentType = contentType)
    }

    fun HttpContext.favoriteArticle(
        users: Store<User, String>, articles: Store<Article, String>, favorite: Boolean
    ): HttpContext {

        val principal = attributes["principal"] as DecodedJWT
        val slug = pathParameters.require("slug")
        val article = articles.findOne(slug) ?: return notFound()
        val author = checkNotNull(users.findOne(article.author))
        val user = checkNotNull(users.findOne(principal.subject)) // Both can be fetched with one 'find'
        val updatedAt = LocalDateTime.now()
        val pair = Article::updatedAt.name to updatedAt
        val favoritedBy =
            if (favorite) article.favoritedBy + principal.subject
            else article.favoritedBy - principal.subject
        val updates = mapOf(Article::favoritedBy.name to favoritedBy)

        if (!articles.updateOne(slug, updates + pair))
            return internalServerError()

        val favoritedArticle = article.copy(favoritedBy = favoritedBy)

        val articleResponseRoot = ArticleResponseRoot(favoritedArticle, author, user)
        val body = articleResponseRoot.serialize(APPLICATION_JSON)
        return ok(body, contentType = contentType)
    }

    fun HttpContext.getArticle(
        jwt: Jwt, users: Store<User, String>, articles: Store<Article, String>): HttpContext {

        val principal = jwt.parsePrincipal(this)
        val article = articles.findOne(pathParameters.require("slug")) ?: return notFound()
        val author = checkNotNull(users.findOne(article.author))
        val user = users.findOne(principal?.subject ?: "")

        return ok(ArticleResponseRoot(article, author, user).serialize(APPLICATION_JSON), contentType = contentType)
    }

    fun HttpContext.updateArticle(jwt: Jwt, articles: Store<Article, String>): HttpContext {
        val principal = jwt.parsePrincipal(this) ?: return unauthorized("Unauthorized")
        val body = request.bodyMap().requirePath<Map<String,Any>>("article").let(::PutArticleRequest)
        val slug = pathParameters.require("slug")

        val updatedAt = LocalDateTime.now()
        val updatedAtPair = Article::updatedAt.name to updatedAt
        val requestUpdates = body.toFieldsMap().mapKeys { it.key } + updatedAtPair

        val updates =
            if (body.title != null) requestUpdates + (Article::slug.name to body.title.toSlug())
            else requestUpdates

        val updated = articles.updateOne(slug, updates)

        return if (updated) {
            val article = checkNotNull(articles.findOne(slug))
            val content = ArticleCreationResponseRoot(article, principal.subject).serialize(APPLICATION_JSON)
            ok(content, contentType = contentType)
        }
        else {
            internalServerError("Article $slug not updated")
        }
    }

    fun HttpContext.deleteArticle(jwt: Jwt, articles: Store<Article, String>): HttpContext {
        jwt.parsePrincipal(this) ?: return unauthorized("Unauthorized")
        val slug = pathParameters.require("slug")
        return if (!articles.deleteOne(slug))
            notFound("Article $slug not found")
        else
            ok(OkResponse("Article $slug deleted").serialize(APPLICATION_JSON), contentType = contentType)
    }

    fun HttpContext.getFeed(jwt: Jwt, users: Store<User, String>, articles: Store<Article, String>): HttpContext {
        val principal = jwt.parsePrincipal(this) ?: return unauthorized("Unauthorized")
        val user = users.findOne(principal.subject) ?: return notFound()

        val feedArticles = if(user.following.isEmpty()) {
            ArticlesResponseRoot(emptyList(), 0)
        }
        else {
            val filter = mapOf(Article::author.name to (user.following.toList()))
            searchArticles(users, articles, principal.subject, filter)
        }

        return ok(feedArticles.serialize(APPLICATION_JSON), contentType = contentType)
    }

    fun String.toSlug() =
        this.lowercase().replace(' ', '-')

    fun HttpContext.searchArticles(
        users: Store<User, String>,
        articles: Store<Article, String>,
        subject: String?,
        filter: Map<String, *>
    ): ArticlesResponseRoot {

        val sort = mapOf(Article::createdAt.name to false)
        val queryParameters = request.queryParameters
        val limit = queryParameters["limit"]?.string()?.toInt() ?: 20
        val offset = queryParameters["offset"]?.string()?.toInt() ?: 0
        val allArticles = articles.findMany(filter, limit, offset, sort)
        val userNames = allArticles.map { it.author } + subject
        val authors = users.findMany(mapOf(User::username.name to userNames))
        val authorsMap = authors.associateBy { it.username }
        val user = authorsMap[subject]
        val responses = allArticles.map {
            val authorUsername = it.author
            val author = authorsMap[authorUsername]
            ArticleResponse(
                slug = it.slug,
                title = it.title,
                description = it.description,
                body = it.body,
                tagList = it.tagList,
                createdAt = it.createdAt.withZone(ZoneId.of("Z")).format(ISO_ZONED_DATE_TIME),
                updatedAt = it.updatedAt.withZone(ZoneId.of("Z")).format(ISO_ZONED_DATE_TIME),
                favorited = it.favoritedBy.contains(subject),
                favoritesCount = it.favoritedBy.size,
                author = AuthorResponse(
                    username = authorUsername,
                    bio = author?.bio ?: "",
                    image = author?.image?.toString() ?: "",
                    following = user?.following?.contains(authorUsername) ?: false
                )
            )
        }

        return ArticlesResponseRoot(responses, articles.count(filter))
    }

    fun getFeed1(
        subject: String,
        users: Store<User, String>,
        articles: Store<Article, String>,
        limit: Int,
        offset: Int,
    ): List<Article> {
        val user = users.findOne(subject) ?: return emptyList()

        val feedArticles = if(user.following.isEmpty()) {
            emptyList()
        }
        else {
            val filter = mapOf(Article::author.name to (user.following.toList()))
            searchArticles1(users, articles, subject, filter, limit, offset)
        }

        return feedArticles
    }

    fun searchArticles1(
        users: Store<User, String>,
        articles: Store<Article, String>,
        subject: String?,
        filter: Map<String, *>,
        limit: Int,
        offset: Int,
    ): List<Article> {

        val sort = mapOf(Article::createdAt.name to false)
        val allArticles = articles.findMany(filter, limit, offset, sort)
        val userNames = allArticles.map { it.author } + subject
        val authors = users.findMany(mapOf(User::username.name to userNames))
        val authorsMap = authors.associateBy { it.username }
        val user = authorsMap[subject]

        return allArticles
    }
}

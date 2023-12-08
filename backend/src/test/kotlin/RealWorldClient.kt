package com.hexagonkt.realworld

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.core.requirePath
import com.hexagonkt.http.client.HttpClient
import com.hexagonkt.http.client.HttpClientSettings
import com.hexagonkt.http.client.jetty.JettyClientAdapter
import com.hexagonkt.http.model.*
import com.hexagonkt.realworld.rest.messages.*
import com.hexagonkt.realworld.rest.contentType
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.rest.bodyMap
import com.hexagonkt.serialization.serialize
import java.time.ZonedDateTime
import kotlin.test.assertEquals

internal class RealWorldClient(val client: HttpClient) {

    init {
        client.start()
    }

    private fun User.toRegistrationRequest(): RegistrationRequest =
        RegistrationRequest(email, username, password)

    private fun User.toLoginRequest(): LoginRequest =
        LoginRequest(email, password)

    private fun Article.toCreationRequest(): ArticleRequest =
        ArticleRequest(title, description, body, tagList)

    fun deleteUser(user: User, allowedCodes: Set<Int> = setOf(200, 404)) {
        client.delete("/users/${user.username}").apply {
            assert(status.code in allowedCodes) { "${status.code} not in $allowedCodes" }
            assertEquals(contentType, contentType)
        }
    }

    fun registerUser(user: User) {
        registerUser(user) {
            assertEquals(CREATED_201, status)

            val userResponse = UserResponse(bodyMap().requirePath("user"))
            assertEquals(user.username, userResponse.username)
            assertEquals(user.email, userResponse.email)
            assert(userResponse.token.isNotBlank())
        }
    }

    fun registerUser(user: User, callback: HttpResponsePort.() -> Unit) {
        client.post("/users", mapOf("user" to user.toRegistrationRequest()).serialize(APPLICATION_JSON)).apply(callback)
    }

    fun loginUser(user: User): RealWorldClient {
        val header = client.post("/users/login", mapOf("user" to user.toLoginRequest()).serialize(APPLICATION_JSON)).let {
            assertEquals(OK_200, it.status)
            assertEquals(contentType, it.contentType)

            val userResponse = UserResponse(it.bodyMap().requirePath("user"))
            assertEquals(user.username, userResponse.username)
            assertEquals(user.email, userResponse.email)
            assert(userResponse.token.isNotBlank())

            userResponse.token
        }

        val settings = HttpClientSettings(
            client.settings.baseUrl,
            client.settings.contentType,
            authorization = Authorization("token", header),
        )
        val userClient = HttpClient(JettyClientAdapter(), settings)
        return RealWorldClient(userClient)
    }

    fun initializeUser(user: User): RealWorldClient {
        deleteUser(user)
        registerUser(user)
        return loginUser(user)
    }

    fun getUser(user: User) {
        getUser(user) {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)

            val userResponse = UserResponse(bodyMap().requirePath("user"))
            assertEquals(user.username, userResponse.username)
            assertEquals(user.email, userResponse.email)
            assert(userResponse.token.isNotBlank())
        }
    }

    fun getUser(user: User, callback: HttpResponsePort.(User) -> Unit) {
        client.get("/user").apply { callback(user) }
    }

    fun updateUser(user: User, updateRequest: PutUserRequest) {
        updateUser(user, updateRequest) {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)

            val userResponse = UserResponse(bodyMap().requirePath("user"))
            assertEquals(user.username, userResponse.username)
            assertEquals((updateRequest.email ?: user.email), userResponse.email)
            assert(userResponse.token.isNotBlank())
        }
    }

    fun updateUser(user: User, updateRequest: PutUserRequest, callback: HttpResponsePort.(User) -> Unit) {
        client.put("/user", mapOf("user" to updateRequest).serialize(APPLICATION_JSON)).apply { callback(user) }
    }

    fun getProfile(user: User, following: Boolean) {
        client.get("/profiles/${user.username}").apply {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)

            val profileResponse = ProfileResponse(bodyMap())
            assertEquals(user.username, profileResponse.username)
            assertEquals(following, profileResponse.following)
        }
    }

    fun followProfile(user: User, follow: Boolean) {
        val url = "/profiles/${user.username}/follow"
        val response = if (follow) client.post(url) else client.delete(url)

        response.apply {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)

            val profileResponse = ProfileResponse(bodyMap())
            assertEquals(user.username, profileResponse.username)
            assertEquals(follow, profileResponse.following)
        }
    }

    fun postArticle(article: Article) {
        client.post("/articles", mapOf("article" to article.toCreationRequest()).serialize(APPLICATION_JSON)).apply {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)

            val postArticleResponse = ArticleCreationResponse(bodyMap().requirePath("article"))
            // TODO Check all timestamps' formats
            ZonedDateTime.parse(postArticleResponse.createdAt)
            assertEquals(article.body, postArticleResponse.body)
            assertEquals(article.slug, postArticleResponse.slug)
            assertEquals(article.description, postArticleResponse.description)
            assert(!postArticleResponse.favorited)
            assertEquals(0, postArticleResponse.favoritesCount)
        }
    }

    fun getArticle(slug: String) {
        client.get("/articles/$slug").apply {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)

            val getArticleResponse = ArticleResponse(bodyMap().requirePath("article"))
            assertEquals(slug, getArticleResponse.slug)
        }
    }

    fun deleteArticle(slug: String) {
        client.delete("/articles/$slug").apply {
            assert(status in setOf(OK_200, NOT_FOUND_404))
            assertEquals(contentType, contentType)

            if (status == OK_200)
                assertEquals("Article $slug deleted", OkResponse(bodyMap().requirePath("message")).message)
            else
                assertEquals("Article $slug not found", ErrorResponse(bodyMap().requirePath("errors", "body")).body.first())
        }
    }

    fun updateArticle(article: Article, updateRequest: PutArticleRequest) {
        client.put("/articles/${article.slug}", mapOf("article" to updateRequest).serialize(APPLICATION_JSON)).apply {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)

            val responseArticle = ArticleCreationResponse(bodyMap().requirePath("article"))
            assertEquals(article.slug, responseArticle.slug)
            assertEquals(updateRequest.title ?: article.title, responseArticle.title)
            assertEquals(updateRequest.description ?: article.description, responseArticle.description)
            assertEquals(updateRequest.body ?: article.body, responseArticle.body)
        }
    }

    fun getFeed(vararg articles: Article) {
        client.get("/articles/feed").apply {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)

            val feedArticles = bodyMap().requirePath<List<Map<*, *>>>("articles").map { ArticleResponse(it) }
            val feedResponse = ArticlesResponseRoot(feedArticles, articles.size.toLong())
            assert(feedResponse.articlesCount >= feedResponse.articles.size)
            assertEquals(articles.size, feedResponse.articles.size)
            assert(feedResponse.articles.all {
                it.slug in articles.map { article -> article.slug }
            })
        }
    }

    fun favoriteArticle(article: Article, favorite: Boolean) {
        val url = "/articles/${article.slug}/favorite"
        val response = if (favorite) client.post(url) else client.delete(url)

        response.apply {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)

            val profileResponse = ArticleResponse(bodyMap().requirePath("article"))
            assertEquals(favorite, profileResponse.favorited)
        }
    }

    fun findArticles(
        author: String? = null,
        favorited: String? = null,
        tag: String? = null,
        expected: Set<Article> = emptySet()) {

        val slugs = expected.map { it.slug }

        findArticles(author, favorited, tag).apply {
            assertEquals(slugs.size, size)
            assert(map { it.slug }.containsAll(slugs))
        }
    }

    fun createComment(article: String, comment: CommentRequest) {
        client.post("/articles/$article/comments", mapOf("comment" to comment).serialize(APPLICATION_JSON)).apply {
            assert(status in setOf(OK_200, NOT_FOUND_404))
            assertEquals(contentType, contentType)

            if (status == OK_200) {
                val commentsResponse = CommentResponse(bodyMap().requirePath("comment"))
                assertEquals(comment.body, commentsResponse.body)
            }
            else if (status == NOT_FOUND_404) {
                val commentsResponse = ErrorResponse(bodyMap().requirePath("errors", "body"))
                assertEquals("$article article not found", commentsResponse.body.first())
            }
        }
    }

    fun deleteComment(article: String, id: Int) {
        client.delete("/articles/$article/comments/$id").apply {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)
            assertEquals("$id deleted", OkResponse(bodyMap().requirePath("message")).message)
        }
    }

    fun getComments(article: String, vararg ids: Int) {
        client.get("/articles/$article/comments").apply {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)

            val commentsResponse = bodyMap().requirePath<List<Map<*, *>>>("comments").map { CommentResponse(it) }
            assertEquals(ids.size, commentsResponse.size)
            assert(commentsResponse.map { it.id }.containsAll(ids.toSet()))
        }
    }

    fun getTags(vararg expectedTags: String) {
        client.get("/tags").apply {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)

            val tags = bodyMap().requirePath<Collection<String>>("tags")
            assertEquals(expectedTags.size, tags.size)
            assert(tags.containsAll(expectedTags.toList().let(::LinkedHashSet)))
        }
    }

    private fun findArticles(
        author: String? = null,
        favorited: String? = null,
        tag: String? = null): List<ArticleResponse> {

        val queryString = mapOf("author" to author, "favorited" to favorited, "tag" to tag)
            .filterValues { it?.isNotBlank() ?: false }
            .map { it.key + "=" + it.value }
            .joinToString("&", "?")

        client.get("/articles$queryString").apply {
            assertEquals(OK_200, status)
            assertEquals(contentType, contentType)

            val articles = bodyMap().requirePath<List<Map<*, *>>>("articles").map { ArticleResponse(it) }
            val articlesRoot = ArticlesResponseRoot(articles, articles.size.toLong())
            assert(articlesRoot.articlesCount >= 0)
            return articles
        }
    }
}
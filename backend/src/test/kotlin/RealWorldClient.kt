package com.hexagonkt.realworld

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.core.requirePath
import com.hexagonkt.http.client.jetty.JettyClientAdapter
import com.hexagonkt.http.model.*
import com.hexagonkt.realworld.rest.messages.*
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.rest.bodyMap
import com.hexagonkt.rest.tools.StateHttpClient
import java.time.ZonedDateTime
import kotlin.test.assertEquals

internal class RealWorldClient(val client: StateHttpClient) {

    companion object {
        val json = ContentType(APPLICATION_JSON)
    }

    constructor(endpoint: String) : this(StateHttpClient(JettyClientAdapter(), endpoint, json))

    init {
        client.start()
    }

    fun deleteUser(user: User, allowedCodes: Set<Int> = setOf(200, 404)) {
        client.delete("/users/${user.username}").apply {
            assert(status.code in allowedCodes) { "${status.code} not in $allowedCodes" }
            assertEquals(client.contentType, contentType)
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
        client.post("/users", mapOf("user" to user.toRegistrationRequest())).apply(callback)
    }

    fun loginUser(user: User): RealWorldClient {
        val header = client.post("/users/login", mapOf("user" to user.toLoginRequest())).let {
            assertEquals(OK_200, it.status)
            assertEquals(ContentType(APPLICATION_JSON, charset = Charsets.UTF_8), it.contentType)

            val userResponse = UserResponse(it.bodyMap().requirePath("user"))
            assertEquals(user.username, userResponse.username)
            assertEquals(user.email, userResponse.email)
            assert(userResponse.token.isNotBlank())

            userResponse.token
        }

        return RealWorldClient(
            StateHttpClient(
                client.adapter,
                client.url,
                json,
                authorization = Authorization("token", header)
            )
        )
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

    fun updateUser(
        user: User, updateRequest: PutUserRequest, callback: HttpResponsePort.(User) -> Unit
    ) {
        client.put("/user", mapOf("user" to updateRequest)).apply { callback(user) }
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
        client.post("/articles", mapOf("article" to article.toCreationRequest())).apply {
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
        client.put("/articles/${article.slug}", mapOf("article" to updateRequest)).apply {
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

            val feedArticles = bodyMap().requirePath<List<Map<String, *>>>("articles").map { ArticleResponse(it) }
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
        client.post("/articles/$article/comments", mapOf("comment" to comment)).apply {
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

            val commentsResponse = bodyMap().requirePath<List<Map<String, *>>>("comments").map { CommentResponse(it) }
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

            val articles = bodyMap().requirePath<List<Map<String, *>>>("articles").map { ArticleResponse(it) }
            val articlesRoot = ArticlesResponseRoot(articles, articles.size.toLong())
            assert(articlesRoot.articlesCount >= 0)
            return articles
        }
    }

    private fun User.toRegistrationRequest(): RegistrationRequest =
        RegistrationRequest(email, username, password)

    private fun User.toLoginRequest(): LoginRequest =
        LoginRequest(email, password)

    private fun Article.toCreationRequest(): ArticleRequest =
        ArticleRequest(title, description, body, tagList)
}

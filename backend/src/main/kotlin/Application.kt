package com.hexagonkt.realworld

import com.hexagonkt.core.logging.LoggingManager
import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.http.handlers.FilterHandler
import com.hexagonkt.http.handlers.HttpHandler
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.server.*
import com.hexagonkt.http.server.jetty.JettyServletAdapter
import com.hexagonkt.logging.slf4j.jul.Slf4jJulLoggingAdapter
import com.hexagonkt.realworld.rest.*
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.serialization.SerializationManager
import com.hexagonkt.serialization.jackson.json.Json
import com.hexagonkt.store.Store

data class Application(
    private val settings: Settings = Settings(),
    private val jwt: Jwt,
    private val users: Store<User, String>,
    private val articles: Store<Article, String>,
    private val contentType: ContentType = ContentType(APPLICATION_JSON, charset = Charsets.UTF_8)
) {
    private val authenticator = FilterHandler("*") {
        val principal = jwt.parsePrincipal(this)

        if (principal == null) next()
        else send(attributes = attributes + ("principal" to principal)).next()
    }

    private val serverSettings = HttpServerSettings(settings.bindAddress, settings.bindPort, "/api")
    private val serverAdapter = JettyServletAdapter()
    private val userRouter = UserRouter(jwt, users, contentType, authenticator).userRouter
    private val usersRouter = UsersRouter(jwt, users, contentType).usersRouter
    private val profilesRouter = ProfilesRouter(users, contentType, authenticator).profilesRouter
    private val commentsRouter = CommentsRouter(jwt, users, articles, contentType).commentsRouter
    private val articlesRouter = ArticlesRouter(jwt, users, articles, contentType, authenticator, commentsRouter).articlesRouter
    private val tagsRouter = TagsRouter(articles, contentType).tagsRouter
    private val router: HttpHandler by lazy { Routes(jwt, articles, userRouter, usersRouter, profilesRouter, articlesRouter, tagsRouter).router }
    internal val server: HttpServer by lazy { HttpServer(serverAdapter, router, serverSettings) }

    init {
        LoggingManager.adapter = Slf4jJulLoggingAdapter()
        SerializationManager.defaultFormat = Json
    }

    fun start() {
        server.start()
    }
}

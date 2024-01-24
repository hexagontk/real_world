package com.hexagonkt.realworld

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.http.handlers.FilterHandler
import com.hexagonkt.http.handlers.HttpHandler
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.server.*
import com.hexagonkt.http.server.netty.NettyServerAdapter
import com.hexagonkt.realworld.domain.UsersService
import com.hexagonkt.realworld.rest.*
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.store.Store
import kotlin.text.Charsets.UTF_8

data class Application(
    private val settings: Settings = Settings(),
    private val jwt: Jwt,
    private val users: Store<User, String>,
    private val articles: Store<Article, String>,
    private val contentType: ContentType = ContentType(APPLICATION_JSON, charset = UTF_8)
) {
    private val authenticator = FilterHandler("*") {
        val principal = jwt.parsePrincipal(this)

        if (principal == null) next()
        else send(attributes = attributes + ("principal" to principal)).next()
    }

    private val serverSettings = HttpServerSettings(settings.bindAddress, settings.bindPort, "/api")
    private val serverAdapter = NettyServerAdapter()
    private val usersService = UsersService(users)
    private val userRouter = UserRouter(jwt, usersService, contentType, authenticator)
    private val usersRouter = UsersRouter(jwt, users, contentType)
    private val profilesRouter = ProfilesRouter(users, contentType, authenticator)
    private val commentsRouter = CommentsRouter(jwt, users, articles, contentType)
    private val articlesRouter = ArticlesRouter(jwt, users, articles, contentType, authenticator, commentsRouter)
    private val tagsRouter = TagsRouter(articles, contentType)
    private val router: HttpHandler by lazy { Routes(userRouter, usersRouter, profilesRouter, articlesRouter, tagsRouter) }
    internal val server: HttpServer by lazy { HttpServer(serverAdapter, router, serverSettings) }

    fun start() {
        server.start()
    }
}

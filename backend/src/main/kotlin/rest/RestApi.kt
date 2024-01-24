package com.hexagonkt.realworld.rest

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.http.handlers.FilterHandler
import com.hexagonkt.http.handlers.HttpHandler
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.server.*
import com.hexagonkt.http.server.netty.NettyServerAdapter
import com.hexagonkt.realworld.Jwt
import com.hexagonkt.realworld.Settings
import com.hexagonkt.realworld.domain.UsersService
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.store.Store
import kotlin.text.Charsets.UTF_8

data class RestApi(
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
    private val userController = UserController(jwt, usersService, contentType, authenticator)
    private val usersController = UsersController(jwt, users, contentType)
    private val profilesController = ProfilesController(users, contentType, authenticator)
    private val commentsController = CommentsController(jwt, users, articles, contentType)
    private val articlesController = ArticlesController(jwt, users, articles, contentType, authenticator, commentsController)
    private val tagsController = TagsController(articles, contentType)
    private val router: HttpHandler by lazy { ApiController(userController, usersController, profilesController, articlesController, tagsController) }
    internal val server: HttpServer by lazy { HttpServer(serverAdapter, router, serverSettings) }

    fun start() {
        server.start()
    }
}

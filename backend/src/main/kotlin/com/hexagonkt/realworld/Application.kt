package com.hexagonkt.realworld

import com.hexagonkt.core.Jvm.systemSetting
import com.hexagonkt.core.converters.ConvertersManager
import com.hexagonkt.core.getString
import com.hexagonkt.core.getStrings
import com.hexagonkt.core.getStringsOrEmpty
import com.hexagonkt.core.logging.LoggingManager
import com.hexagonkt.core.requireString
import com.hexagonkt.http.server.*
import com.hexagonkt.http.server.jetty.JettyServletAdapter
import com.hexagonkt.http.server.servlet.ServletServer
import com.hexagonkt.logging.logback.LogbackLoggingAdapter
import com.hexagonkt.realworld.routes.*
import com.hexagonkt.realworld.services.Article
import com.hexagonkt.realworld.services.User
import com.hexagonkt.serialization.SerializationManager
import com.hexagonkt.serialization.jackson.json.Json
import com.hexagonkt.store.Store
import com.hexagonkt.store.mongodb.MongoDbStore
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import java.net.URL
import jakarta.servlet.annotation.WebListener

/**
 * This class is the application's Servlet shell. It allows this application to be bundled
 * in a WAR file and be deployed in any JEE server.
 */
@WebListener
@Suppress("unused")
class WebApplication : ServletServer(router)

internal val serverSettings = HttpServerSettings(contextPath = "/api")
internal val serverAdapter = JettyServletAdapter()
internal val server: HttpServer by lazy { HttpServer(serverAdapter, router, serverSettings) }
internal val jwt: Jwt by lazy { createJwt() }
internal val users: Store<User, String> by lazy { createUserStore() }
internal val articles: Store<Article, String> by lazy { createArticleStore() }

internal fun createJwt(): Jwt {
    val keyStoreResource = systemSetting<String>("keyStoreResource")
    val keyStorePassword = systemSetting<String>("keyStorePassword")
    val keyPairAlias = systemSetting<String>("keyPairAlias")

    return Jwt(URL(keyStoreResource), keyStorePassword, keyPairAlias)
}

internal fun createUserStore(): Store<User, String> {
    val mongodbUrl = systemSetting<String>("mongodbUrl")
    val userStore = MongoDbStore(User::class, User::username, mongodbUrl)
    val indexField = User::email.name
    val indexOptions = IndexOptions().unique(true).background(true).name(indexField)
    userStore.collection.createIndex(Indexes.ascending(indexField), indexOptions)

    return userStore
}

internal fun createArticleStore(): Store<Article, String> {
    val mongodbUrl = systemSetting<String>("mongodbUrl")
    val articleStore = MongoDbStore(Article::class, Article::slug, mongodbUrl)
    val indexField = Article::author.name
    val indexOptions = IndexOptions().unique(false).background(true).name(indexField)
    articleStore.collection.createIndex(Indexes.ascending(indexField), indexOptions)

    return articleStore
}

internal fun main() {
    LoggingManager.adapter = LogbackLoggingAdapter()
    SerializationManager.defaultFormat = Json
    ConvertersManager.register(User::class to Map::class) { it.toMap() }
    ConvertersManager.register(Map::class to User::class) {
        User(
            username = it.requireString(User::username),
            email = it.requireString(User::email),
            password = it.requireString(User::password),
            bio = it.getString(User::bio),
            image = it.getString(User::image)?.let(::URL),
            following = it.getStringsOrEmpty(User::following).toSet(),
        )
    }
    server.start()
}

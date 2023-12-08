package com.hexagonkt.realworld.rest

import com.auth0.jwt.interfaces.DecodedJWT
import com.hexagonkt.core.MultipleException
import com.hexagonkt.core.fail
import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.http.handlers.FilterHandler
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.model.HttpStatus
import com.hexagonkt.http.server.callbacks.CorsCallback
import com.hexagonkt.http.server.callbacks.LoggingCallback
import com.hexagonkt.http.handlers.HttpContext
import com.hexagonkt.http.handlers.path
import com.hexagonkt.realworld.jwt
import com.hexagonkt.realworld.rest.messages.ErrorResponse
import com.hexagonkt.realworld.rest.messages.ErrorResponseRoot
import com.hexagonkt.realworld.Jwt
import com.hexagonkt.serialization.serialize
import kotlin.text.Charsets.UTF_8

val contentType: ContentType = ContentType(APPLICATION_JSON, charset = UTF_8)
val allowedHeaders: Set<String> = setOf("accept", "user-agent", "host", "content-type")

internal val router by lazy {
    path {
        filter("*", callback = LoggingCallback(includeBody = false, includeHeaders = false))
        filter("*", callback = CorsCallback(allowedHeaders = allowedHeaders))

        after("*") {
            if (status.code in setOf(401, 403, 404)) statusCodeHandler(status, response.body)
            else this
        }

        path("/users", usersRouter)
        path("/user", userRouter)
        path("/profiles/{username}", profilesRouter)
        path("/articles", articlesRouter)
        path("/tags", tagsRouter)

        exception(Exception::class) { exceptionHandler(exception ?: fail) }
        exception<MultipleException>(clear = false) { multipleExceptionHandler(exception ?: fail) }
    }
}

internal fun HttpContext.statusCodeHandler(status: HttpStatus, body: Any): HttpContext {
    val messages = when (body) {
        is List<*> -> body.mapNotNull { it?.toString() }
        else -> listOf(body.toString())
    }

    return send(status, ErrorResponseRoot(ErrorResponse(messages)).serialize(APPLICATION_JSON), contentType = contentType)
}

internal fun HttpContext.multipleExceptionHandler(error: Exception): HttpContext {
    return if (error is MultipleException) {
        val messages = error.causes.map { it.message ?: "<no message>" }
        internalServerError(ErrorResponseRoot(ErrorResponse(messages)), contentType = contentType)
    }
    else this
}

internal fun HttpContext.exceptionHandler(error: Exception): HttpContext {
    val errorMessage = error.javaClass.simpleName + ": " + (error.message ?: "<no message>")
    val errorResponseRoot = ErrorResponseRoot(ErrorResponse(listOf(errorMessage)))
    return internalServerError(errorResponseRoot.serialize(APPLICATION_JSON), contentType = contentType)
}

val authenticator = FilterHandler("*") {
    val principal = parsePrincipal(jwt)

    if (principal == null) next()
    else send(attributes = attributes + ("principal" to principal)).next()
}

internal fun HttpContext.parsePrincipal(jwt: Jwt): DecodedJWT? {
    val token = request.authorization

    return if (token == null) {
        null
    }
    else {
        jwt.verify(token.value)
    }
}
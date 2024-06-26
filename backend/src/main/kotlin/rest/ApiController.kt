package com.hexagonkt.realworld.rest

import com.hexagonkt.core.MultipleException
import com.hexagonkt.core.fail
import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.http.handlers.HttpContext
import com.hexagonkt.http.handlers.HttpController
import com.hexagonkt.http.handlers.HttpHandler
import com.hexagonkt.http.handlers.path
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.model.HttpStatus
import com.hexagonkt.http.server.callbacks.CorsCallback
import com.hexagonkt.http.server.callbacks.LoggingCallback
import com.hexagonkt.realworld.rest.messages.ErrorResponse
import com.hexagonkt.realworld.rest.messages.ErrorResponseRoot
import com.hexagonkt.rest.SerializeResponseCallback
import kotlin.text.Charsets.UTF_8

internal data class ApiController(
    private val userRouter: HttpHandler,
    private val usersRouter: HttpHandler,
    private val profilesRouter: HttpHandler,
    private val articlesRouter: HttpHandler,
    private val tagsRouter: HttpHandler,
) : HttpController {

    val contentType: ContentType = ContentType(APPLICATION_JSON, charset = UTF_8)
    val allowedHeaders: Set<String> = setOf("accept", "user-agent", "host", "content-type")

    override val handler by lazy {
        path {
            filter("*", callback = LoggingCallback(includeBody = false, includeHeaders = false))
            filter("*", callback = CorsCallback(allowedHeaders = allowedHeaders))
            after("*", callback = SerializeResponseCallback())

            exception<Exception> { exceptionHandler(exception ?: fail) }
            exception<MultipleException> { multipleExceptionHandler(exception ?: fail) }

            after("*") {
                if (status.code in setOf(401, 403, 404)) statusCodeHandler(status, response.body)
                else this
            }

            path("/users", usersRouter)
            path("/user", userRouter)
            path("/profiles/{username}", profilesRouter)
            path("/articles", articlesRouter)
            path("/tags", tagsRouter)
        }
    }

    internal fun HttpContext.statusCodeHandler(status: HttpStatus, body: Any): HttpContext {
        val messages = when (body) {
            is List<*> -> body.mapNotNull { it?.toString() }
            else -> listOf(body.toString())
        }

        return send(status, ErrorResponseRoot(ErrorResponse(messages)), contentType = contentType)
    }

    internal fun HttpContext.multipleExceptionHandler(error: Exception): HttpContext {
        return if (error is MultipleException) {
            val messages = error.causes.map { it.message ?: "<no message>" }
            internalServerError(
                ErrorResponseRoot(ErrorResponse(messages)),
                contentType = contentType
            )
        }
        else exceptionHandler(error)
    }

    internal fun HttpContext.exceptionHandler(error: Exception): HttpContext {
        val errorMessage = error.javaClass.simpleName + ": " + (error.message ?: "<no message>")
        val errorResponseRoot = ErrorResponseRoot(ErrorResponse(listOf(errorMessage)))
        return internalServerError(errorResponseRoot, contentType = contentType)
    }
}

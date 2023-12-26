package com.hexagonkt.realworld.rest

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.core.requirePath
import com.hexagonkt.http.handlers.HttpContext
import com.hexagonkt.http.handlers.HttpHandler
import com.hexagonkt.http.handlers.path
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.realworld.rest.messages.PutUserRequest
import com.hexagonkt.realworld.rest.messages.UserResponseRoot
import com.hexagonkt.realworld.Jwt
import com.hexagonkt.realworld.domain.UsersService
import com.hexagonkt.rest.bodyMap
import com.hexagonkt.serialization.serialize

internal data class UserRouter(
    private val jwt: Jwt,
    private val users: UsersService,
    private val contentType: ContentType,
    private val authenticator: HttpHandler,
) {
    val userRouter = path {
        use(authenticator)

        get {
            val subject = jwt.parsePrincipal(this) ?: return@get unauthorized("Unauthorized")
            findUser(subject)
        }

        put {
            val subject = jwt.parsePrincipal(this) ?: return@put unauthorized("Unauthorized")
            val body = PutUserRequest(request.bodyMap().requirePath<Map<*, *>>("user"))
            val updates = body.toFieldsMap()

            val updated = users.replaceUser(subject, updates)

            if (updated) findUser(subject)
            else internalServerError("Username $subject not updated")
        }
    }

    private fun HttpContext.findUser(subject: String): HttpContext {
        val user = users.searchUser(subject) ?: return notFound("User: $subject not found")
        val token = jwt.sign(user.username)

        return ok(
            UserResponseRoot(user, token).serialize(APPLICATION_JSON),
            contentType = contentType
        )
    }
}

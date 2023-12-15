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
import com.hexagonkt.rest.bodyMap
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.serialization.serialize
import com.hexagonkt.store.Store

internal data class UserRouter(
    private val jwt: Jwt,
    private val users: Store<User, String>,
    private val contentType: ContentType,
    private val authenticator: HttpHandler,
) {
    val userRouter by lazy {
        path {
            use(authenticator)

            get {
                getUser(users, jwt)
            }

            put {
                val principal = jwt.parsePrincipal(this) ?: return@put unauthorized("Unauthorized")
                val body = PutUserRequest(request.bodyMap().requirePath<Map<*, *>>("user"))
                val updates = body.toFieldsMap()

                val updated = users.updateOne(principal.subject, updates)

                if (updated)
                    getUser(users, jwt)
                else
                    internalServerError("Username ${principal.subject} not updated")
            }
        }
    }

    fun HttpContext.getUser(users: Store<User, String>, jwt: Jwt): HttpContext {
        val principal = jwt.parsePrincipal(this) ?: return unauthorized("Unauthorized")
        val subject = principal.subject
        val user = users.findOne(subject) ?: return notFound("User: $subject not found")
        val token = jwt.sign(user.username)

        return ok(
            UserResponseRoot(user, token).serialize(APPLICATION_JSON),
            contentType = contentType
        )
    }
}

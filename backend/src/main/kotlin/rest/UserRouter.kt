package com.hexagonkt.realworld.rest

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.core.requirePath
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
    val userRouter = path {
        use(authenticator)

        get {
            val principal = jwt.parsePrincipal(this) ?: return@get unauthorized("Unauthorized")
            val subject = principal.subject
            val user = users.findOne(subject) ?: return@get notFound("User: $subject not found")
            val token = jwt.sign(user.username)

            ok(
                UserResponseRoot(user, token).serialize(APPLICATION_JSON),
                contentType = contentType
            )
        }

        put {
            val principal = jwt.parsePrincipal(this) ?: return@put unauthorized("Unauthorized")
            val body = PutUserRequest(request.bodyMap().requirePath<Map<*, *>>("user"))
            val updates = body.toFieldsMap()

            val updated = users.updateOne(principal.subject, updates)

            if (updated) {
                val subject = principal.subject
                val user = users.findOne(subject) ?: return@put notFound("User: $subject not found")
                val token = jwt.sign(user.username)

                ok(
                    UserResponseRoot(user, token).serialize(APPLICATION_JSON),
                    contentType = contentType
                )
            }
            else {
                internalServerError("Username ${principal.subject} not updated")
            }
        }
    }
}

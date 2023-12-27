package com.hexagonkt.realworld.rest

import com.hexagonkt.core.require
import com.hexagonkt.core.requirePath
import com.hexagonkt.http.handlers.path
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.realworld.Jwt
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.realworld.rest.messages.*
import com.hexagonkt.rest.bodyMap
import com.hexagonkt.store.Store

internal data class UsersRouter(
    private val jwt: Jwt,
    private val users: Store<User, String>,
    private val contentType: ContentType,
) {
    internal val usersRouter = path {
        // TODO Authenticate and require 'root' user or owner
        delete("/{username}") {
            val username = pathParameters.require("username")
            val deleteOne = users.deleteOne(username)
            if (deleteOne)
                ok(OkResponse("$username deleted"), contentType = contentType)
            else
                notFound("$username not found")
        }

        post("/login") {
            val bodyUser = LoginRequest(request.bodyMap().requirePath("user"))
            val filter = mapOf(User::email.name to bodyUser.email)
            val user = users.findOne(filter) ?: return@post notFound("Not Found")
            if (user.password == bodyUser.password)
                ok(UserResponseRoot(user, jwt.sign(user.username)), contentType = contentType)
            else
                unauthorized("Bad credentials")
        }

        post {
            val user = RegistrationRequest(request.bodyMap().requirePath("user"))

            val key = users.insertOne(User(user.username, user.email, user.password))
            val content = UserResponseRoot(
                UserResponse(
                    email = user.email,
                    username = key,
                    bio = "",
                    image = "",
                    token = jwt.sign(key)
                )
            )

            created(content, contentType = contentType)
        }
    }
}

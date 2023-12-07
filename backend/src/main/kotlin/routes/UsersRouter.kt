package com.hexagonkt.realworld.routes

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.core.require
import com.hexagonkt.core.requirePath
import com.hexagonkt.http.handlers.HttpContext
import com.hexagonkt.http.handlers.path
import com.hexagonkt.realworld.jwt
import com.hexagonkt.realworld.messages.*
import com.hexagonkt.realworld.Jwt
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.realworld.users
import com.hexagonkt.rest.bodyMap
import com.hexagonkt.serialization.serialize
import com.hexagonkt.store.Store

internal val usersRouter by lazy {
    path {
        delete("/{username}") { deleteUser(users) }
        post("/login") { login(users, jwt) }
        post { register(users, jwt) }
    }
}

private fun HttpContext.register(users: Store<User, String>, jwt: Jwt): HttpContext {
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
    ).serialize(APPLICATION_JSON)

    return created(content, contentType = contentType)
}

private fun HttpContext.login(users: Store<User, String>, jwt: Jwt): HttpContext {
    val bodyUser = LoginRequest(request.bodyMap().requirePath("user"))
    val filter = mapOf(User::email.name to bodyUser.email)
    val user = users.findOne(filter) ?: return notFound("Not Found")
    return if (user.password == bodyUser.password) {
        val content = UserResponseRoot(user, jwt.sign(user.username)).serialize(APPLICATION_JSON)
        ok(content, contentType = contentType)
    } else {
         unauthorized("Bad credentials")
    }
}

// TODO Authenticate and require 'root' user or owner
private fun HttpContext.deleteUser(users: Store<User, String>): HttpContext {
    val username = pathParameters.require("username")
    val deleteOne = users.deleteOne(username)
    return if (deleteOne)
        ok(OkResponse("$username deleted").serialize(APPLICATION_JSON), contentType = contentType)
    else
        notFound("$username not found")
}

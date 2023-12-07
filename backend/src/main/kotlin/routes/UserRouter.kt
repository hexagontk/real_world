package com.hexagonkt.realworld.routes

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.core.requirePath
import com.hexagonkt.http.handlers.HttpContext
import com.hexagonkt.http.handlers.path
import com.hexagonkt.realworld.jwt
import com.hexagonkt.realworld.messages.PutUserRequest
import com.hexagonkt.realworld.messages.UserResponseRoot
import com.hexagonkt.realworld.Jwt
import com.hexagonkt.rest.bodyMap
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.realworld.users
import com.hexagonkt.serialization.serialize
import com.hexagonkt.store.Store

internal val userRouter by lazy {
    path {
        use(authenticator)
        get { getUser(users, jwt) }
        put { putUser(users, jwt) }
    }
}

internal fun HttpContext.putUser(users: Store<User, String>, jwt: Jwt): HttpContext {
    val principal = parsePrincipal(jwt) ?: return unauthorized("Unauthorized")
    val body = PutUserRequest(request.bodyMap().requirePath<Map<*,*>>("user"))
    val updates = body.toFieldsMap()

    val updated = users.updateOne(principal.subject, updates)

    return if (updated)
        getUser(users, jwt)
    else
        internalServerError("Username ${principal.subject} not updated")
}

internal fun HttpContext.getUser(users: Store<User, String>, jwt: Jwt): HttpContext {
    val principal = parsePrincipal(jwt) ?: return unauthorized("Unauthorized")
    val subject = principal.subject
    val user = users.findOne(subject) ?: return notFound("User: $subject not found")
    val token = jwt.sign(user.username)

    return ok(UserResponseRoot(user, token).serialize(APPLICATION_JSON), contentType = contentType)
}

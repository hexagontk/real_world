package com.hexagonkt.realworld.routes

import com.auth0.jwt.interfaces.DecodedJWT
import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.core.require
import com.hexagonkt.http.handlers.HttpContext
import com.hexagonkt.http.handlers.path
import com.hexagonkt.realworld.messages.ProfileResponse
import com.hexagonkt.realworld.messages.ProfileResponseRoot
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.realworld.users
import com.hexagonkt.serialization.serialize
import com.hexagonkt.store.Store

internal val profilesRouter by lazy {
    path {
        use(authenticator)
        post("/follow") { followProfile(users, true) }
        delete("/follow") { followProfile(users, false) }
        get { getProfile(users) }
    }
}

private fun HttpContext.getProfile(users: Store<User, String>): HttpContext {
    val principal = attributes["principal"] as DecodedJWT
    val user = users.findOne(principal.subject) ?: return notFound("Not Found")
    val profile = users.findOne(pathParameters.require("username")) ?: return notFound("Not Found")
    val content = ProfileResponseRoot(
        ProfileResponse(
            username = profile.username,
            bio = profile.bio ?: "",
            image = profile.image?.toString() ?: "",
            following = user.following.contains(profile.username)
        )
    )

    return ok(content.serialize(APPLICATION_JSON), contentType = contentType)
}

private fun HttpContext.followProfile(
    users: Store<User, String>, follow: Boolean): HttpContext {

    val principal = attributes["principal"] as DecodedJWT
    val user = users.findOne(principal.subject) ?: return notFound("Not Found")
    val followingList =
        if (follow) user.following + pathParameters["username"]
        else user.following - pathParameters["username"]
    val updated = users.updateOne(principal.subject, mapOf("following" to followingList))
    if (!updated)
        return internalServerError()
    val profile = users.findOne(pathParameters.require("username")) ?: return notFound("Not Found")
    val content = ProfileResponseRoot(
        ProfileResponse(
            username = profile.username,
            bio = profile.bio ?: "",
            image = profile.image?.toString() ?: "",
            following = follow
        )
    )

    return ok(content.serialize(APPLICATION_JSON), contentType = contentType)
}

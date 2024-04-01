package com.hexagonkt.realworld.rest

import com.hexagonkt.core.require
import com.hexagonkt.http.handlers.HttpContext
import com.hexagonkt.http.handlers.HttpController
import com.hexagonkt.http.handlers.HttpHandler
import com.hexagonkt.http.handlers.path
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.realworld.rest.messages.ProfileResponse
import com.hexagonkt.realworld.rest.messages.ProfileResponseRoot
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.store.Store

internal data class ProfilesController(
    private val users: Store<User, String>,
    private val contentType: ContentType,
    private val authenticator: HttpHandler,
) : HttpController {

    override val handler by lazy {
        path {
            use(authenticator)
            post("/follow") { followProfile(users, true) }
            delete("/follow") { followProfile(users, false) }
            get { getProfile(users) }
        }
    }

    private fun HttpContext.getProfile(users: Store<User, String>): HttpContext {
        val subject = attributes["principal"] as String
        val user = users.findOne(subject) ?: return notFound("Not Found")
        val profile =
            users.findOne(pathParameters.require("username")) ?: return notFound("Not Found")
        val content = ProfileResponseRoot(
            ProfileResponse(
                username = profile.username,
                bio = profile.bio ?: "",
                image = profile.image?.toString() ?: "",
                following = user.following.contains(profile.username)
            )
        )

        return ok(content, contentType = contentType)
    }

    private fun HttpContext.followProfile(
        users: Store<User, String>, follow: Boolean
    ): HttpContext {

        val subject = attributes["principal"] as String
        val user = users.findOne(subject) ?: return notFound("Not Found")
        val followingList =
            if (follow) user.following + pathParameters["username"]
            else user.following - pathParameters["username"]
        val updated = users.updateOne(subject, mapOf("following" to followingList))
        if (!updated)
            return internalServerError()
        val profile =
            users.findOne(pathParameters.require("username")) ?: return notFound("Not Found")
        val content = ProfileResponseRoot(
            ProfileResponse(
                username = profile.username,
                bio = profile.bio ?: "",
                image = profile.image?.toString() ?: "",
                following = follow
            )
        )

        return ok(content, contentType = contentType)
    }
}

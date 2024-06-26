package com.hexagonkt.realworld.rest.messages

import com.hexagonkt.core.requirePath

data class ProfileResponse(
    val username: String,
    val bio: String,
    val image: String,
    val following: Boolean
) {
    constructor(data: Map<String, *>) : this(
        data.requirePath("profile", ProfileResponse::username),
        data.requirePath("profile", ProfileResponse::bio),
        data.requirePath("profile", ProfileResponse::image),
        data.requirePath("profile", ProfileResponse::following),
    )
}

data class ProfileResponseRoot(val profile: ProfileResponse)

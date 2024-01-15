package com.hexagonkt.realworld.domain.model

import java.net.URI

data class User(
    val username: String,
    val email: String,
    val password: String,
    val bio: String? = null,
    val image: URI? = null,
    val following: Set<String> = emptySet()
)

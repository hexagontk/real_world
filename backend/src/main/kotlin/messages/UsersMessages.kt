package com.hexagonkt.realworld.messages

import com.hexagonkt.core.requirePath

data class RegistrationRequest(
    val email: String,
    val username: String,
    val password: String
) {
    constructor(data: Map<*, *>) : this(
        data.requirePath(RegistrationRequest::email),
        data.requirePath(RegistrationRequest::username),
        data.requirePath(RegistrationRequest::password),
    )
}

data class LoginRequest(
    val email: String,
    val password: String
) {
    constructor(data: Map<*, *>) : this(
        data.requirePath(LoginRequest::email),
        data.requirePath(LoginRequest::password),
    )
}

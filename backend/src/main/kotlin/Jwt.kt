package com.hexagonkt.realworld

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.hexagonkt.core.security.getPrivateKey
import com.hexagonkt.core.security.getPublicKey
import com.hexagonkt.core.security.loadKeyStore
import com.hexagonkt.http.handlers.HttpContext
import java.net.URL

class Jwt(keyStoreResource: URL, password: String, private val alias: String) {
    private val keyStore = loadKeyStore(keyStoreResource, password)
    private val privateKey = keyStore.getPrivateKey(alias, password)
    private val publicKey = keyStore.getPublicKey(alias)
    private val algorithm: Algorithm = Algorithm.RSA256(publicKey, privateKey)
    private val verifier = JWT.require(algorithm).withIssuer(alias).build()

    fun sign(subject: String): String =
        JWT.create().withIssuer(alias).withSubject(subject).sign(algorithm)

    fun verify(token: String): String =
        verifier.verify(token).subject

    fun parsePrincipal(context: HttpContext): String? =
        context.request.authorization?.let { verify(it.value) }
}

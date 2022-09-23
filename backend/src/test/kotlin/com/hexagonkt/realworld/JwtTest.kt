package com.hexagonkt.realworld

import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.test.assertEquals

class JwtTest {

    @Test
    fun `JWT creation and parsing works properly`() {
        val jwt = Jwt(URL("classpath:keystore.p12"), "storepass", "realWorld")
        val token = jwt.sign("subject")

        assertEquals("subject", jwt.verify(token).subject)
    }
}

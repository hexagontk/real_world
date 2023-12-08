package com.hexagonkt.realworld

import com.hexagonkt.core.urlOf
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JwtTest {

    @Test fun `JWT creation and parsing works properly`() {
        val jwt = Jwt(urlOf("classpath:keystore.p12"), "storepass", "realWorld")
        val token = jwt.sign("subject")

        assertEquals("subject", jwt.verify(token).subject)
    }
}

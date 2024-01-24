package com.hexagonkt.realworld.rest.it

import com.hexagonkt.http.model.NOT_FOUND_404
import com.hexagonkt.realworld.RealWorldClient
import com.hexagonkt.realworld.restApi
import com.hexagonkt.realworld.domain.model.User
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import java.net.URI
import kotlin.test.assertEquals

/**
 * TODO Test bad requests (invalid JSON, bad field formats, etc.)
 */
@DisabledIfEnvironmentVariable(named = "DOCKER_BUILD", matches = "true")
internal class ApiControllerIT : ITBase() {

    private val jake = User(
        username = "jake",
        email = "jake@jake.jake",
        password = "jakejake",
        bio = "I work at statefarm",
        image = URI("https://i.pravatar.cc/150?img=3")
    )

    @Test fun `Non existing route returns a 404`() {
        val client = RealWorldClient("http://localhost:${restApi.server.runtimePort}/api")
        val jakeClient = client.initializeUser(jake)

        jakeClient.client.get("/404").apply { assertEquals(NOT_FOUND_404, status) }
    }
}

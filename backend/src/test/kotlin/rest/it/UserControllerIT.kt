package com.hexagonkt.realworld.rest.it

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.core.requirePath
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.model.UNAUTHORIZED_401
import com.hexagonkt.realworld.RealWorldClient
import com.hexagonkt.realworld.restApi
import com.hexagonkt.realworld.rest.messages.ErrorResponse
import com.hexagonkt.realworld.rest.messages.PutUserRequest
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.rest.bodyMap
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import java.net.URI
import kotlin.test.assertEquals

@DisabledIfEnvironmentVariable(named = "DOCKER_BUILD", matches = "true")
internal class UserControllerIT : ITBase() {

    private val jake = User(
        username = "jake",
        email = "jake@jake.jake",
        password = "jakejake",
        bio = "I work at statefarm",
        image = URI("https://i.pravatar.cc/150?img=3")
    )

    @Test fun `Get and update current user`() {
        val client = RealWorldClient("http://localhost:${restApi.server.runtimePort}/api")
        val jakeClient = client.initializeUser(jake)

        jakeClient.getUser(jake)
        jakeClient.updateUser(jake, PutUserRequest(email = jake.email))
        jakeClient.updateUser(jake, PutUserRequest(email = "changed.${jake.email}"))

        client.getUser(jake) {
            val errors = ErrorResponse(bodyMap().requirePath("errors", "body"))
            assertEquals(UNAUTHORIZED_401, status)
            assertEquals(ContentType(APPLICATION_JSON, charset = Charsets.UTF_8), contentType)
            assert(errors.body.isNotEmpty())
            assertEquals("Unauthorized", errors.body.first())
        }

        client.updateUser(jake, PutUserRequest(email = jake.email)) {
            val errors = ErrorResponse(bodyMap().requirePath("errors", "body"))
            assertEquals(UNAUTHORIZED_401, status)
            assertEquals(ContentType(APPLICATION_JSON, charset = Charsets.UTF_8), contentType)
            assert(errors.body.isNotEmpty())
        }
    }
}

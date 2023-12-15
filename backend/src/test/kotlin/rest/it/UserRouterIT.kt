package com.hexagonkt.realworld.rest.it

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.core.urlOf
import com.hexagonkt.core.requirePath
import com.hexagonkt.http.client.HttpClient
import com.hexagonkt.http.client.HttpClientSettings
import com.hexagonkt.http.client.jetty.JettyClientAdapter
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.model.UNAUTHORIZED_401
import com.hexagonkt.realworld.RealWorldClient
import com.hexagonkt.realworld.application
import com.hexagonkt.realworld.main
import com.hexagonkt.realworld.rest.messages.ErrorResponse
import com.hexagonkt.realworld.rest.messages.PutUserRequest
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.rest.bodyMap
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import kotlin.test.assertEquals

@TestInstance(PER_CLASS)
class UserRouterIT {

    private val jake = User(
        username = "jake",
        email = "jake@jake.jake",
        password = "jakejake",
        bio = "I work at statefarm",
        image = urlOf("https://i.pravatar.cc/150?img=3")
    )

    @BeforeAll fun startup() {
        System.setProperty("mongodbUrl", mongodbUrl)

        main()
    }

    @AfterAll fun shutdown() {
        application.server.stop()
    }

    @Test fun `Get and update current user`() {
        val endpoint = urlOf("http://localhost:${application.server.runtimePort}/api")
        val settings = HttpClientSettings(endpoint, ContentType(APPLICATION_JSON))
        val client = RealWorldClient(HttpClient(JettyClientAdapter(), settings))

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

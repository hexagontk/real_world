package com.hexagonkt.realworld.routes.it

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.http.client.HttpClient
import com.hexagonkt.http.client.HttpClientSettings
import com.hexagonkt.http.client.jetty.JettyClientAdapter
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.model.NOT_FOUND_404
import com.hexagonkt.realworld.RealWorldClient
import com.hexagonkt.realworld.main
import com.hexagonkt.realworld.server
import com.hexagonkt.realworld.domain.model.User
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.net.URL
import kotlin.test.assertEquals

/**
 * TODO Test bad requests (invalid JSON, bad field formats, etc.)
 */
@TestInstance(PER_CLASS)
class RoutesIT {

    private val jake = User(
        username = "jake",
        email = "jake@jake.jake",
        password = "jakejake",
        bio = "I work at statefarm",
        image = URL("https://i.pravatar.cc/150?img=3")
    )

    @BeforeAll fun startup() {
        System.setProperty("mongodbUrl", mongodbUrl)

        main()
    }

    @AfterAll fun shutdown() {
        server.stop()
    }

    @Test fun `Non existing route returns a 404`() {
        val endpoint = URL("http://localhost:${server.runtimePort}/api")
        val settings = HttpClientSettings(endpoint, ContentType(APPLICATION_JSON))
        val client = RealWorldClient(HttpClient(JettyClientAdapter(), settings))

        val jakeClient = client.initializeUser(jake)

        jakeClient.client.get("/404").apply { assertEquals(NOT_FOUND_404, status) }
    }
}

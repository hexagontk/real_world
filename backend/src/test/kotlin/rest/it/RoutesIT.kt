package com.hexagonkt.realworld.rest.it

import com.hexagonkt.core.urlOf
import com.hexagonkt.http.model.NOT_FOUND_404
import com.hexagonkt.realworld.RealWorldClient
import com.hexagonkt.realworld.application
import com.hexagonkt.realworld.main
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.serialization.SerializationManager
import com.hexagonkt.serialization.jackson.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
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
        image = urlOf("https://i.pravatar.cc/150?img=3")
    )

    @BeforeAll fun startup() {
        SerializationManager.formats = setOf(Json)
        System.setProperty("mongodbUrl", mongodbUrl)

        main()
    }

    @AfterAll fun shutdown() {
        application.server.stop()
    }

    @Test fun `Non existing route returns a 404`() {
        val client = RealWorldClient("http://localhost:${application.server.runtimePort}/api")
        val jakeClient = client.initializeUser(jake)

        jakeClient.client.get("/404").apply { assertEquals(NOT_FOUND_404, status) }
    }
}

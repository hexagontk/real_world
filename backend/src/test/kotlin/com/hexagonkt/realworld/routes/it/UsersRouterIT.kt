package com.hexagonkt.realworld.routes.it

import com.hexagonkt.core.Jvm
import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.core.requireKeys
import com.hexagonkt.http.client.HttpClient
import com.hexagonkt.http.client.HttpClientSettings
import com.hexagonkt.http.client.jetty.JettyClientAdapter
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.model.INTERNAL_SERVER_ERROR_500
import com.hexagonkt.realworld.RealWorldClient
import com.hexagonkt.realworld.main
import com.hexagonkt.realworld.messages.ErrorResponse
import com.hexagonkt.realworld.server
import com.hexagonkt.realworld.services.User
import com.hexagonkt.rest.bodyMap
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.net.URL
import kotlin.test.assertEquals

/**
 * TODO
 *   - Login without credentials
 *   - Login with bad password
 */
@TestInstance(PER_CLASS)
class UsersRouterIT {

    private val port = Jvm.systemSettingOrNull<Int>("realWorldPort")

    private val jake = User(
        username = "jake",
        email = "jake@jake.jake",
        password = "jakejake",
        bio = "I work at statefarm",
        image = URL("https://i.pravatar.cc/150?img=3")
    )

    @BeforeAll fun startup() {
        if (port != null)
            return

        System.setProperty("mongodbUrl", mongodbUrl)
        main()
    }

    @AfterAll fun shutdown() {
        server.stop()
    }

    @Test fun `Delete, login and register users`() {
        val endpoint = URL("http://localhost:${port ?: server.runtimePort}/api")
        val settings = HttpClientSettings(endpoint, ContentType(APPLICATION_JSON))
        val client = RealWorldClient(HttpClient(JettyClientAdapter(), settings))

        client.deleteUser(jake)
        client.deleteUser(jake, setOf(404))
        client.registerUser(jake)
        client.registerUser(jake) {
            assertEquals(INTERNAL_SERVER_ERROR_500, status)
            assertEquals(contentType, contentType)

            val errors = ErrorResponse(bodyMap().requireKeys("errors", "body"))
            val exception = "MongoWriteException: Write operation error on server localhost"
            val message = "WriteError{code=11000, message='E11000 duplicate key error collection"
            val key = """real_world.User index: _id_ dup key: { _id: "${jake.username}" }', details={}}."""
            val errorMessage = errors.body.first()
            assert(errorMessage.contains(exception))
            assert(errorMessage.contains(message))
            assert(errorMessage.contains(key))
        }

        client.loginUser(jake)
        client.loginUser(jake) // Login ok two times in a row should work
    }
}

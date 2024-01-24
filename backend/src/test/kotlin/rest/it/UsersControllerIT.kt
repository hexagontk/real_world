package com.hexagonkt.realworld.rest.it

import com.hexagonkt.core.requirePath
import com.hexagonkt.http.model.INTERNAL_SERVER_ERROR_500
import com.hexagonkt.realworld.RealWorldClient
import com.hexagonkt.realworld.restApi
import com.hexagonkt.realworld.rest.messages.ErrorResponse
import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.rest.bodyMap
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import java.net.URI
import kotlin.test.assertEquals

/**
 * TODO
 *   - Login without credentials
 *   - Login with bad password
 */
@DisabledIfEnvironmentVariable(named = "DOCKER_BUILD", matches = "true")
internal class UsersControllerIT : ITBase() {

    private val jake = User(
        username = "jake",
        email = "jake@jake.jake",
        password = "jakejake",
        bio = "I work at statefarm",
        image = URI("https://i.pravatar.cc/150?img=3")
    )

    @Test fun `Delete, login and register users`() {
        val client = RealWorldClient("http://localhost:${restApi.server.runtimePort}/api")

        client.deleteUser(jake)
        client.deleteUser(jake, setOf(404))
        client.registerUser(jake)
        client.registerUser(jake) {
            assertEquals(INTERNAL_SERVER_ERROR_500, status)
            assertEquals(contentType, contentType)

            val errors = ErrorResponse(bodyMap().requirePath("errors", "body"))
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

package com.hexagonkt.realworld.rest.it

import com.hexagonkt.realworld.RealWorldClient
import com.hexagonkt.realworld.application
import com.hexagonkt.realworld.domain.model.User
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import java.net.URI

@DisabledIfEnvironmentVariable(named = "DOCKER_BUILD", matches = "true")
internal class ProfilesRouterIT : ITBase() {

    private val jake = User(
        username = "jake",
        email = "jake@jake.jake",
        password = "jakejake",
        bio = "I work at statefarm",
        image = URI("https://i.pravatar.cc/150?img=3")
    )

    private val jane = User(
        username = "jane",
        email = "jane@jane.jane",
        password = "janejane",
        bio = "I own MegaCloud",
        image = URI("https://i.pravatar.cc/150?img=1")
    )

    @Test fun `Follow and unfollow a profile`() {
        val client = RealWorldClient("http://localhost:${application.server.runtimePort}/api")
        val jakeClient = client.initializeUser(jake)
        val janeClient = client.initializeUser(jane)

        jakeClient.getProfile(jane, false)
        jakeClient.followProfile(jane, true)
        jakeClient.getProfile(jane, true)
        jakeClient.followProfile(jane, false)
        jakeClient.getProfile(jane, false)

        janeClient.getProfile(jake, false)
        janeClient.followProfile(jake, true)
        janeClient.getProfile(jake, true)
        janeClient.followProfile(jake, false)
        janeClient.getProfile(jake, false)
    }
}

package com.hexagonkt.realworld.rest.it

import com.hexagonkt.core.urlOf
import com.hexagonkt.realworld.RealWorldClient
import com.hexagonkt.realworld.application
import com.hexagonkt.realworld.main
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.realworld.domain.model.User
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
class TagsIT {

    private val jake = User(
        username = "jake",
        email = "jake@jake.jake",
        password = "jakejake",
        bio = "I work at statefarm",
        image = urlOf("https://i.pravatar.cc/150?img=3")
    )

    private val neverEndingStory = Article(
        title = "Never Ending Story",
        slug = "never-ending-story",
        description = "Fantasia is dying",
        body = "Fight for Fantasia!",
        tagList = linkedSetOf("dragons", "books"),
        author = jake.username
    )

    private val trainDragon = Article(
        title = "How to train your dragon",
        slug = "how-to-train-your-dragon",
        description = "Ever wonder how?",
        body = "Very carefully.",
        tagList = linkedSetOf("dragons", "training"),
        author = jake.username
    )

    @BeforeAll fun startup() {
        System.setProperty("mongodbUrl", mongodbUrl)

        main()
    }

    @AfterAll fun shutdown() {
        application.server.stop()
    }

    @Test fun `Get all tags don't return duplicates`() {
        val client = RealWorldClient("http://localhost:${application.server.runtimePort}/api")
        val jakeClient = client.initializeUser(jake)

        jakeClient.deleteArticle(trainDragon.slug)
        jakeClient.deleteArticle(neverEndingStory.slug)
        client.getTags()

        jakeClient.postArticle(trainDragon)
        client.getTags("dragons", "training")

        jakeClient.postArticle(neverEndingStory)
        client.getTags("dragons", "training", "books")

        jakeClient.deleteArticle(trainDragon.slug)
        client.getTags("dragons", "books")

        jakeClient.deleteArticle(neverEndingStory.slug)
        client.getTags()
    }
}

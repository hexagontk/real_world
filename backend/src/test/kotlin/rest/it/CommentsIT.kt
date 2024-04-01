package com.hexagonkt.realworld.rest.it

import com.hexagonkt.realworld.RealWorldClient
import com.hexagonkt.realworld.restApi
import com.hexagonkt.realworld.rest.messages.CommentRequest
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.realworld.domain.model.User
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import java.net.URI

@DisabledIfEnvironmentVariable(named = "DOCKER_BUILD", matches = "true")
internal class CommentsIT : ITBase() {

    private val jake = User(
        username = "jake",
        email = "jake@jake.jake",
        password = "jakejake",
        bio = "I work at statefarm",
        image = URI("https://i.pravatar.cc/150?img=3")
    )

    private val trainDragon = Article(
        title = "How to train your dragon",
        slug = "how-to-train-your-dragon",
        description = "Ever wonder how?",
        body = "Very carefully.",
        tagList = linkedSetOf("dragons", "training"),
        author = jake.username
    )

    @Test fun `Delete, create and get article's comments`() {
        val client = RealWorldClient("http://localhost:${restApi.server.runtimePort}/api")
        val jakeClient = client.initializeUser(jake)

        jakeClient.deleteArticle(trainDragon.slug)
        jakeClient.postArticle(trainDragon)

        jakeClient.createComment(trainDragon.slug, CommentRequest("Nice film"))
        jakeClient.getComments(trainDragon.slug, 1)
        jakeClient.deleteComment(trainDragon.slug, 1)
    }

    @Test fun `Get article's comments without login`() {
        val client = RealWorldClient("http://localhost:${restApi.server.runtimePort}/api")
        val jakeClient = client.initializeUser(jake)

        jakeClient.deleteArticle(trainDragon.slug)
        jakeClient.postArticle(trainDragon)

        jakeClient.createComment(trainDragon.slug, CommentRequest("Nice film"))
        client.getComments(trainDragon.slug, 1)
        jakeClient.createComment(trainDragon.slug, CommentRequest("Not bad"))
        client.getComments(trainDragon.slug, 1, 2)
    }

    @Test fun `Post comment to a not created article`() {
        val client = RealWorldClient("http://localhost:${restApi.server.runtimePort}/api")
        val jakeClient = client.initializeUser(jake)

        jakeClient.createComment("non_existing_article", CommentRequest("Nice film"))
    }
}

package com.hexagonkt.realworld.rest.it

import com.hexagonkt.realworld.RealWorldClient
import com.hexagonkt.realworld.application
import com.hexagonkt.realworld.rest.messages.PutArticleRequest
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.realworld.domain.model.User
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import java.net.URI

// TODO Add test to check articles' tags order
@DisabledIfEnvironmentVariable(named = "DOCKER_BUILD", matches = "true")
internal class ArticlesIT : ITBase() {

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

    private val trainDragon = Article(
        title = "How to train your dragon",
        slug = "how-to-train-your-dragon",
        description = "Ever wonder how?",
        body = "Very carefully.",
        tagList = linkedSetOf("dragons", "training"),
        author = jake.username
    )

    private val neverEndingStory = Article(
        title = "Never Ending Story",
        slug = "never-ending-story",
        description = "Fantasia is dying",
        body = "Fight for Fantasia!",
        tagList = linkedSetOf("dragons", "books"),
        author = jake.username
    )

    @Test fun `Delete, create update and get an article`() {
        val client = RealWorldClient("http://localhost:${application.server.runtimePort}/api")
        val jakeClient = client.initializeUser(jake)

        jakeClient.deleteArticle(trainDragon.slug)
        jakeClient.postArticle(trainDragon)
        jakeClient.updateArticle(trainDragon, PutArticleRequest())
        jakeClient.updateArticle(trainDragon, PutArticleRequest(body = "With your bare hands"))
        jakeClient.getArticle(trainDragon.slug)
    }

    @Test fun `Favorite and un-favorite articles`() {
        val client = RealWorldClient("http://localhost:${application.server.runtimePort}/api")
        val user = jake.username

        val jakeClient = client.initializeUser(jake)
        val janeClient = client.initializeUser(jane)

        janeClient.deleteArticle(trainDragon.slug)
        janeClient.deleteArticle(neverEndingStory.slug)
        janeClient.postArticle(trainDragon)
        janeClient.postArticle(neverEndingStory)

        jakeClient.findArticles(favorited = user, expected = emptySet())

        jakeClient.favoriteArticle(trainDragon, true)
        jakeClient.findArticles(favorited = user, expected = setOf(trainDragon))

        jakeClient.favoriteArticle(neverEndingStory, true)
        jakeClient.findArticles(favorited = user, expected = setOf(trainDragon, neverEndingStory))

        jakeClient.favoriteArticle(trainDragon, false)
        jakeClient.findArticles(favorited = user, expected = setOf(neverEndingStory))

        jakeClient.favoriteArticle(neverEndingStory, false)
        jakeClient.findArticles(favorited = user, expected = emptySet())
    }

    @Test fun `Find articles filters correctly`() {
        val client = RealWorldClient("http://localhost:${application.server.runtimePort}/api")
        val jakeClient = client.initializeUser(jake)
        val janeClient = client.initializeUser(jane)

        jakeClient.deleteArticle(trainDragon.slug)
        jakeClient.deleteArticle(neverEndingStory.slug)

        jakeClient.postArticle(trainDragon)
        janeClient.postArticle(neverEndingStory)
        jakeClient.favoriteArticle(neverEndingStory, true)
        janeClient.favoriteArticle(trainDragon, true)

        val clients = listOf(client, jakeClient, janeClient)

        clients.forEach {
            it.findArticles(author = "jake", expected = setOf(trainDragon))
            it.findArticles(author = "jane", expected = setOf(neverEndingStory))
            it.findArticles(author = "john", expected = emptySet())
        }

        clients.forEach {
            it.findArticles(tag = "dragons", expected = setOf(trainDragon, neverEndingStory))
            it.findArticles(tag = "training", expected = setOf(trainDragon))
            it.findArticles(tag = "books", expected = setOf(neverEndingStory))
            it.findArticles(tag = "other", expected = emptySet())
        }

        clients.forEach {
            it.findArticles(favorited = jake.username, expected = setOf(neverEndingStory))
            it.findArticles(favorited = jane.username, expected = setOf(trainDragon))
            it.findArticles(favorited = "john", expected = emptySet())
        }
    }

    @Test fun `Get user feed`() {
        val client = RealWorldClient("http://localhost:${application.server.runtimePort}/api")
        val jakeClient = client.initializeUser(jake)
        val janeClient = client.initializeUser(jane)

        janeClient.deleteArticle(trainDragon.slug)
        janeClient.deleteArticle(neverEndingStory.slug)

        jakeClient.getFeed()
        jakeClient.followProfile(jane, true)
        jakeClient.getFeed()
        janeClient.postArticle(trainDragon)
        jakeClient.getFeed(trainDragon)
        janeClient.postArticle(neverEndingStory)
        jakeClient.getFeed(trainDragon, neverEndingStory)
        janeClient.deleteArticle(trainDragon.slug)
        jakeClient.getFeed(neverEndingStory)
    }
}

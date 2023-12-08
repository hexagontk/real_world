package com.hexagonkt.realworld.rest

import com.hexagonkt.core.media.APPLICATION_JSON
import com.hexagonkt.http.handlers.path
import com.hexagonkt.realworld.createArticleStore
import com.hexagonkt.realworld.rest.messages.TagsResponseRoot
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.serialization.serialize
import com.hexagonkt.store.Store

internal val tagsRouter by lazy {
    path {
        val articles: Store<Article, String> = createArticleStore()

        get {
            val field = Article::tagList.name
            val tags = articles.findAll(listOf(field))
                .flatMap {
                    it[field]?.let { tags ->
                        if (tags is Collection<*>)
                            tags.map { tag -> tag.toString() }
                        else
                            null
                    } ?: emptyList()
                }
                .distinct()

            ok(TagsResponseRoot(tags).serialize(APPLICATION_JSON), contentType = contentType)
        }
    }
}

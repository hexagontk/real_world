package com.hexagonkt.realworld.rest

import com.hexagonkt.http.handlers.HttpController
import com.hexagonkt.http.handlers.path
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.realworld.rest.messages.TagsResponseRoot
import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.store.Store

internal data class TagsRouter(
    private val articles: Store<Article, String>,
    private val contentType: ContentType,
) : HttpController {

    override val handler by lazy {
        path {
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

                ok(TagsResponseRoot(tags), contentType = contentType)
            }
        }
    }
}

package com.hexagonkt.realworld.domain

import com.hexagonkt.realworld.domain.model.Article
import com.hexagonkt.store.Store

data class ArticlesService(
    internal val articles: Store<Article, String>,
) {
}

package com.hexagonkt.realworld.rest.it

import org.testcontainers.containers.MongoDBContainer

private val mongoDb: MongoDBContainer = MongoDBContainer("mongo:7-jammy")
    .withExposedPorts(27017)
    .apply { start() }

internal val mongodbUrl by lazy {
    "mongodb://localhost:${mongoDb.getMappedPort(27017)}/real_world"
}

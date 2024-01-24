package com.hexagonkt.realworld.rest.it

import com.hexagonkt.core.Jvm
import com.hexagonkt.realworld.application
import com.hexagonkt.realworld.main
import com.hexagonkt.serialization.SerializationManager
import com.hexagonkt.serialization.jackson.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.testcontainers.containers.MongoDBContainer

@TestInstance(PER_CLASS)
internal open class ITBase {

    private val port = Jvm.systemSettingOrNull<Int>("realWorldPort")

    private val mongoDb: MongoDBContainer by lazy {
        MongoDBContainer("mongo:7-jammy")
            .withExposedPorts(27017)
            .apply { start() }
    }

    private val mongodbUrl by lazy {
        "mongodb://localhost:${mongoDb.getMappedPort(27017)}/real_world"
    }

    @BeforeAll fun startup() {
        SerializationManager.formats = setOf(Json)
        if (port != null)
            return
        System.setProperty("mongodbUrl", mongodbUrl)

        main()
    }

    @AfterAll fun shutdown() {
        application.server.stop()
    }
}

import org.graalvm.buildtools.gradle.dsl.GraalVMExtension

plugins {
    application
    war
}

apply(from = "${properties["gradleScripts"]}/kotlin.gradle")
apply(from = "${properties["gradleScripts"]}/application.gradle")
apply(from = "${properties["gradleScripts"]}/native.gradle")

extensions.configure<JavaApplication> {
    mainClass.set("com.hexagonkt.realworld.MainKt")
}

tasks.named("assemble") {
    dependsOn("installDist")
}

dependencies {
    val hexagonVersion = properties["hexagonVersion"]
    val hexagonExtraVersion = properties["hexagonExtraVersion"]
    val javaJwtVersion = properties["javaJwtVersion"]
    val testcontainersVersion = properties["testcontainersVersion"]
    val mockkVersion = properties["mockkVersion"]
    val archUnitVersion = properties["archUnitVersion"]

    "implementation"("com.hexagonkt:serialization_jackson_json:$hexagonVersion")
    "implementation"("com.hexagonkt:http_server_netty:$hexagonVersion")
    "implementation"("com.hexagonkt:rest:$hexagonVersion")
    "implementation"("com.hexagonkt.extra:store_mongodb:$hexagonExtraVersion")
    "implementation"("com.auth0:java-jwt:$javaJwtVersion")

    "testImplementation"("com.tngtech.archunit:archunit-junit5:$archUnitVersion")
    "testImplementation"("com.hexagonkt:rest_tools:$hexagonVersion")
    "testImplementation"("com.hexagonkt:http_client_jetty:$hexagonVersion")
    "testImplementation"("org.testcontainers:mongodb:$testcontainersVersion")
    "testImplementation"("io.mockk:mockk:$mockkVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
}

extensions.configure<GraalVMExtension> {
    fun option(name: String, value: (String) -> String): String? =
        System.getProperty(name)?.let(value)

    binaries {
        named("main") {
            listOfNotNull(
                option("static") { "--static" },
                option("enableMonitoring") { "--enable-monitoring" },
                option("pgoInstrument") { "--pgo-instrument" },
                option("pgo") { "--pgo=../../../default.iprof" },
            )
            .forEach(buildArgs::add)
        }
        named("test") {
            listOfNotNull(
                option("pgoInstrument") { "--pgo-instrument" },
            )
            .forEach(buildArgs::add)
        }
    }
}

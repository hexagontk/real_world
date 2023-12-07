import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL

plugins {
    kotlin("jvm") version("1.9.21") apply(false)

    id("org.graalvm.buildtools.native") version("0.9.28") apply(false)
}

defaultTasks("build")

task("clean", type = Delete::class) {
    delete("build", "log", "out", "kotlin-js-store")

    delete(
        fileTree(rootDir) { include("**/*.log") },
        fileTree(rootDir) { include("**/*.hprof") },
        fileTree(rootDir) { include("**/.attach_pid*") },
        fileTree(rootDir) { include("**/hs_err_pid*") }
    )
}

tasks.wrapper {
    gradleVersion = "8.5"
    distributionType = ALL
}

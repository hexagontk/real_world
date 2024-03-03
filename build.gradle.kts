import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL

plugins {
    kotlin("jvm") version("1.9.22") apply(false)

    id("org.graalvm.buildtools.native") version("0.10.1") apply(false)
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

task("setUp") {
    group = "build setup"
    description = "Set up project for development. Creates the Git pre push hook (run build task)."

    doLast {
        exec { commandLine("docker version".split(" ")) }

        val dotfiles = "https://raw.githubusercontent.com/hexagonkt/.github/master"
        exec { commandLine("curl $dotfiles/commit_template.txt -o .git/message".split(" ")) }
        exec { commandLine("git config commit.template .git/message".split(" ")) }

        val prePush = file(".git/hooks/pre-push")
        file(".github/pre-push.sh").copyTo(prePush, true)
        prePush.setExecutable(true)
    }
}

tasks.wrapper {
    gradleVersion = "8.6"
    distributionType = ALL
}

@file:Suppress("UnstableApiUsage")

val catalogs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val junitVersion by lazy { catalogs.findVersion("junit").get().requiredVersion }

plugins {
    `jvm-test-suite`
}

testing.suites {
    val persistence by registering(JvmTestSuite::class) {
        useJUnitJupiter(junitVersion)
        targets.all {
            dependencies {
                implementation(project())
                implementation(project(":api:testing-persistence"))
                implementation(project(":or-cache"))
            }
            testTask.configure {
                workingDir = rootDir
                jvmArgs("-XX:+EnableDynamicAgentLoading")
                inputs.dir(rootProject.file(".data/cache/SERVER"))
                inputs.file(rootProject.file(".data/gamevals-binary/gamevals.dat"))
                systemProperty("junit.jupiter.execution.parallel.enabled", true)
                systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
                systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
            }
        }
    }
}

@file:Suppress("UnstableApiUsage")

val catalogs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val junitVersion by lazy { catalogs.findVersion("junit").get().requiredVersion }

plugins {
    `jvm-test-suite`
}

testing.suites {
    val integration by registering(JvmTestSuite::class) {
        useJUnitJupiter(junitVersion)
        targets.all {
            dependencies {
                implementation(project())
                implementation(project(":api:testing"))
                implementation(project(":or-cache"))
            }
            testTask.configure {
                workingDir = rootDir
                jvmArgs("-XX:+EnableDynamicAgentLoading")
                systemProperty("junit.jupiter.extensions.autodetection.enabled", true)
                // Autodetection also loads MockKExtension, whose per-test unmockkAll() would
                // tear down the shared ServerCacheManager stub while other tests are still ticking.
                systemProperty("mockk.junit.extension.keepmocks", true)
                systemProperty("junit.jupiter.execution.parallel.enabled", true)
                systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
                systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "same_thread")
            }
        }
    }
}

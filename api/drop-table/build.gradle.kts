plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.kotlin.reflect)
}

// src/test holds vendored drop-table DSL examples with no @Test methods;
// Gradle 9 fails test tasks that discover no tests unless opted out.
tasks.test { failOnNoDiscoveredTests = false }

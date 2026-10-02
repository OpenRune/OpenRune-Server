import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(projects.engine.game)
    implementation(projects.engine.map)
}

// This module's sources are written by the or-cache tasks as a side effect, so the task graph
// has nothing tying the two together. Without this ordering a build that runs both compiles an
// empty source dir while the generator is still working, failing every consumer of api.table.
tasks.withType<KotlinJvmCompile>().configureEach {
    mustRunAfter(":or-cache:buildCache", ":or-cache:freshCache")
}

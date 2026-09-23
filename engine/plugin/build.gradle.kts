plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.classgraph)
    api(libs.guice)
    implementation(libs.kotlin.inline.logger)
    runtimeOnly(libs.logback.classic)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
}

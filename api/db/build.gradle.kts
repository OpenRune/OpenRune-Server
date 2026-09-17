plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.embedded.postgres)
    implementation(libs.openrune.central.common)
    implementation(libs.kotlin.inline.logger)
    runtimeOnly(libs.logback.classic)
    implementation(libs.guice)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.postgresql)
    implementation(projects.api.serverConfig)
    implementation(projects.engine.module)
    implementation(projects.server.services)
}

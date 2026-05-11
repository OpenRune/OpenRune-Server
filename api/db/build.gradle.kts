plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.embedded.postgres)
    implementation(libs.openrune.central.all)
    implementation(libs.bundles.logging)
    implementation(libs.guice)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.postgresql)
    implementation(projects.api.serverConfig)
    implementation(projects.engine.module)
    implementation(projects.server.services)
}

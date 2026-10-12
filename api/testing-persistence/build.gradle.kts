plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.fastutil)
    api(libs.kotlin.coroutines.core)
    api(projects.api.account)
    api(projects.api.db)
    api(projects.engine.game)
    implementation(libs.bundles.logging)
    implementation(libs.embedded.postgres)
    implementation(libs.guice)
    implementation(libs.jupiter.api)
    implementation(libs.postgresql)
    implementation(projects.api.parsers.jackson)
    implementation(projects.api.parsers.json)
    implementation(projects.api.serverConfig)
    implementation(projects.api.testing)
    implementation(projects.engine.module)
}

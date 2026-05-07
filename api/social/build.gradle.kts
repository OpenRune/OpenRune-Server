plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.bundles.logging)
    implementation(libs.rsprot.api)
    implementation(libs.rsprot.shared)
    implementation(libs.guice)
    implementation(projects.api.attr)
    implementation(projects.api.playerOutput)

    implementation(projects.engine.game)
    implementation(projects.api.db)
}

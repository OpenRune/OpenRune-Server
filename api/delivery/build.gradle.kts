plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.bundles.logging)
    implementation(libs.guice)
    implementation(projects.api.attr)
    implementation(projects.api.gameProcess)
    implementation(projects.api.script)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
    implementation(projects.engine.plugin)
    testImplementation(kotlin("test"))
    testImplementation(libs.jackson.databind)
}

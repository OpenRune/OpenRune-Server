plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.classgraph)
    implementation(libs.guice)
    implementation(libs.jackson.dataformat.toml)
    implementation(libs.jackson.module.kotlin)
    implementation(projects.api.death)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.engine.game)
    implementation(projects.engine.plugin)
}

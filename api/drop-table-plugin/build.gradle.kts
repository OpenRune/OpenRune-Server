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
    implementation(projects.api.areaChecker)
    implementation(projects.api.dropTable)
    implementation(projects.api.random)
    implementation(projects.engine.game)
    implementation(projects.engine.plugin)
}

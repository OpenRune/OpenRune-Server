plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.guice)
    implementation(libs.jackson.dataformat.toml)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.inline.logger)
    runtimeOnly(libs.logback.classic)
    implementation(projects.api.areaChecker)
    implementation(projects.api.dropTable)
    implementation(projects.api.random)
    implementation(projects.engine.game)
    implementation(projects.engine.plugin)
}

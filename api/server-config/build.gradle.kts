plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.kotlin.inline.logger)
    runtimeOnly(libs.logback.classic)
    implementation(libs.guice)
    implementation(projects.api.parsers.toml)
    implementation(projects.engine.module)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)
}

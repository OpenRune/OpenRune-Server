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
    implementation(projects.engine.module)
}

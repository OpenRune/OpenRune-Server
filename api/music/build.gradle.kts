plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.fastutil)
    implementation(libs.guice)
    implementation(projects.api.config)
    implementation(projects.api.random)
    implementation(projects.api.tables)
    implementation(projects.engine.game)
    implementation(projects.engine.module)
    implementation(projects.engine.plugin)
}

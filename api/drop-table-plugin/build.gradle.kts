plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.classgraph)
    implementation(libs.guice)
    implementation(projects.api.areaChecker)
    implementation(projects.api.dropTable)
    implementation(projects.api.random)
    implementation(projects.engine.game)
    implementation(projects.engine.plugin)
}

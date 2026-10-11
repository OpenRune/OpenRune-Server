plugins {
    id("base-conventions")
    id("game-cache-test-conventions")

}

kotlin {
    explicitApi()
}

dependencies {
    testImplementation(projects.engine.events)
    testImplementation("org.mockito:mockito-core:5.14.2")
    implementation(libs.fastutil)
    implementation(libs.guice)
    implementation(projects.api.cache)
    implementation(projects.api.config)
    implementation(projects.api.invStorage)
    implementation(projects.api.playerOutput)
    implementation(projects.api.repo)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.objtx)
    implementation(projects.engine.plugin)
}

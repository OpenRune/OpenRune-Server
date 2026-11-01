plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    testImplementation(projects.api.invStorage)
    testImplementation(libs.mockk)
    testImplementation(projects.api.areaChecker)
    testImplementation(projects.api.gameProcess)
    testImplementation(projects.api.market)
    testImplementation(projects.api.random)
    testImplementation(projects.engine.routefinder)
}

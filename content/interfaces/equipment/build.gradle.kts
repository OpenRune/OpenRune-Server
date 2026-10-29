plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    testImplementation(libs.mockk)
    testImplementation(projects.api.invStorage)
    implementation(projects.api.areaChecker)
    implementation(projects.api.death)
    implementation(projects.api.pluginCommons)
}

plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.attr)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.specials)
    testImplementation(projects.api.invStorage)
    testImplementation(libs.mockk)
}

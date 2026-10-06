plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    testImplementation(projects.api.invPlugin)
    testImplementation(projects.api.gameProcess)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.registry)
    testImplementation(libs.fastutil)
    testImplementation(projects.content.generic.genericLocs)
    implementation(projects.api.pluginCommons)
}

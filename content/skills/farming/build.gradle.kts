plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.content.generic.genericLocs)
    implementation(projects.content.quest)
    testImplementation(projects.api.invStorage)
    testImplementation(libs.mockk)
    testImplementation(projects.api.gameProcess)
    testImplementation(libs.fastutil)
    testImplementation(libs.rsprot.api)
}

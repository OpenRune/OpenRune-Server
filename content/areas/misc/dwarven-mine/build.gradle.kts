plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
    id("integration-test-suite")
}

dependencies {
    testImplementation(projects.api.invPlugin)
    testImplementation(projects.api.gameProcess)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.registry)
    testImplementation(libs.fastutil)
    testImplementation(libs.mockk)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.other.dwarfMulticannon)
    implementation(projects.content.quest)
}

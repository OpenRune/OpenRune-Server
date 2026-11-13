plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.generated)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.api.script)
    implementation(projects.api.serverConfig)
    implementation(projects.content.generic.genericLocs)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.quest)
    testImplementation(projects.api.invStorage)
    testImplementation(libs.mockk)
    testImplementation(projects.api.gameProcess)
    testImplementation(libs.fastutil)
    testImplementation(libs.rsprot.api)
}

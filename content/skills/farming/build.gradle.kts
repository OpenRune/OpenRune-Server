plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.generated)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.script)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.gameProcess)
    testImplementation(libs.fastutil)
    testImplementation(libs.rsprot.api)
}

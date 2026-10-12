plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.content.other.pets)
    implementation(projects.content.quest)
    testImplementation(projects.api.invPlugin)
    testImplementation(projects.api.gameProcess)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.registry)
    testImplementation(projects.engine.game)
    testImplementation(projects.engine.map)
    testImplementation(libs.fastutil)
}

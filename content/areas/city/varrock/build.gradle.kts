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
    implementation(projects.api.instances)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.api.script)
    implementation(projects.content.other.pets)
    implementation(projects.content.quest)
    implementation(projects.content.skills.runecrafting)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
}

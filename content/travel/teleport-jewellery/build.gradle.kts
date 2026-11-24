plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.areaChecker)
    implementation(projects.api.death)
    implementation(projects.api.generated)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.content.quest)
    implementation(projects.content.skills.slayer)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.utilsBits)
}

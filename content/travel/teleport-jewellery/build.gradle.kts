plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.areaChecker)
    implementation(projects.api.generated)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.quest)
    implementation(projects.engine.map)
}

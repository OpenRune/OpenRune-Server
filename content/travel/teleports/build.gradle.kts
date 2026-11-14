plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.quest)

    testImplementation(projects.api.invPlugin)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.registry)
}

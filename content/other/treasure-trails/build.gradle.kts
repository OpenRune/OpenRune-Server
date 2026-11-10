plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.areaChecker)
    implementation(projects.api.bosses)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.music)
    implementation(projects.content.drops)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.content.interfaces.emotes)
    implementation(projects.content.quest)
    testImplementation(projects.api.invPlugin)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.registry)
}

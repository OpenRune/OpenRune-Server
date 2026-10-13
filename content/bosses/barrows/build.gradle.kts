plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.combatAchievements)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.areaChecker)
    implementation(projects.api.bosses)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.content.drops)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.content.quest)
    testImplementation(projects.api.invStorage)
    testImplementation("org.mockito:mockito-core:5.14.2")
}

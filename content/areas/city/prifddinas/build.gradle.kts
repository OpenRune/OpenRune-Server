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
    implementation(projects.api.pluginCommons)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.repo)
    implementation(projects.api.shops)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.content.quest)
}

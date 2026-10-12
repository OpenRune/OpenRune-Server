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
    implementation(projects.api.areaChecker)
    implementation(projects.api.death)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.attr)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.repo)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
}

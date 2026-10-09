plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    testImplementation(projects.api.invStorage)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
}

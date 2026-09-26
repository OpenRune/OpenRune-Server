plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.areaChecker)
    implementation(projects.api.bosses)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.content.drops)
    implementation(projects.content.interfaces.collectionLog)
}

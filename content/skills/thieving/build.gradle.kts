plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
    id("integration-test-suite")
}

dependencies {
    testImplementation(projects.api.invStorage)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.content.other.pets)
    integrationImplementation(projects.api.pluginCommons)
    integrationImplementation(projects.api.dropTablePlugin)
    integrationImplementation(projects.content.drops)
    integrationImplementation(libs.guice)
}

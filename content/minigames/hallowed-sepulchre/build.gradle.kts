plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.api.shops)
    implementation(projects.content.drops)
    implementation(projects.content.other.pets)
    implementation(projects.content.quest)
    integrationImplementation(projects.api.generated)
    integrationImplementation(projects.api.hitPlugin)
    integrationImplementation(projects.api.shops)
    integrationImplementation(projects.engine.coroutine)
    integrationImplementation(libs.rsprot.api)
}

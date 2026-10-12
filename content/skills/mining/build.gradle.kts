plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.content.quest)
    implementation(projects.content.skills.utils)
    integrationImplementation(projects.api.generated)
}

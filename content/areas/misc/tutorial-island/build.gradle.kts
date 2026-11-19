plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.attr)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.spells)
    implementation(projects.content.quest)
    integrationImplementation(projects.api.pluginCommons)
    integrationImplementation(projects.content.quest)
}

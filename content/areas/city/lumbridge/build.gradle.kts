plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.content.quest)
    integrationImplementation(projects.api.shops)
    integrationImplementation(projects.engine.game)
}

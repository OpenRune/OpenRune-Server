plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.random)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.quest)
    integrationImplementation(projects.content.interfaces.bank)
}

plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.generated)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.other.gravestones)
    integrationImplementation(projects.api.pluginCommons)
    integrationImplementation(projects.content.other.gravestones)
}

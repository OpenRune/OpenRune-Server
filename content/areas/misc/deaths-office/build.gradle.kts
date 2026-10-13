plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.death)
    implementation(projects.api.generated)
    implementation(projects.api.market)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.content.other.gravestones)
    integrationImplementation(projects.api.pluginCommons)
    integrationImplementation(projects.content.other.gravestones)
    integrationImplementation(projects.api.death)
    integrationImplementation(projects.api.deathPlugin)
    integrationImplementation(projects.api.music)
    integrationImplementation(projects.api.registry)
}

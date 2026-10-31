plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    integrationImplementation(projects.api.pluginCommons)
}

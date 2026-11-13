plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.api.shops)
    implementation(libs.rsprot.api)
    integrationImplementation(projects.api.generated)
    integrationImplementation(projects.api.shops)
    integrationImplementation(projects.engine.coroutine)
    integrationImplementation(libs.rsprot.api)
}

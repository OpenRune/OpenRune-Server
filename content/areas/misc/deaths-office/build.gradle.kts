plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
    id("integration-test-suite")
}

dependencies {
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.invtx)
    testImplementation(projects.api.net)
    testImplementation(libs.mockk)
    testImplementation(libs.rsprot.api)
    testImplementation(libs.fastutil)
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

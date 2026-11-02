plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.events.shootingStars)
    implementation(projects.content.quest)
    integrationImplementation(projects.engine.coroutine)
}

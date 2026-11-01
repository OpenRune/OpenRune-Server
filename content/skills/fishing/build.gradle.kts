plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.api.death)
    implementation(projects.content.quest)
    implementation(projects.content.skills.utils)
    integrationImplementation(projects.api.death)
}

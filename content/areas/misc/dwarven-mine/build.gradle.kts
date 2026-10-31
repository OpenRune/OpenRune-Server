plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.other.dwarfMulticannon)
    implementation(projects.content.quest)
}

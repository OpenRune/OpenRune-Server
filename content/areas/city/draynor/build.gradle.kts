plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.attr)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.quest)
    implementation(projects.api.death)
    implementation(projects.content.interfaces.omnishop)
    testImplementation(projects.content.generic.genericLocs)
    testImplementation(projects.api.hunt)
    testImplementation(libs.fastutil)
    testImplementation(projects.api.registry)
    testImplementation(projects.api.invStorage)
}

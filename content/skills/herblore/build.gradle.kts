plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.content.skills.utils)
    implementation(projects.content.skills.farming)
    testImplementation(projects.api.invStorage)
}

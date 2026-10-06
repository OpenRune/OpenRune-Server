plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.instances)
    implementation(projects.api.invtx)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.api.repo)
    implementation(projects.content.skills.utils)
}

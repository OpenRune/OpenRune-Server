plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.api.config)
    implementation(projects.api.instances)
    implementation(projects.api.player)
    implementation(projects.api.registry)
    implementation(projects.api.repo)
}

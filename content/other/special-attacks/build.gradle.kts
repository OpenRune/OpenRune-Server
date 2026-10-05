plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.npc)
    implementation(projects.api.repo)
    implementation(projects.api.specials)
}

plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.generic.genericLocs)
    implementation(projects.content.quest)
}

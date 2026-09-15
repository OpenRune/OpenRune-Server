plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.skills.utils)
}

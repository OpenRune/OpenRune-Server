plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.content.quest)
    implementation(projects.content.skills.utils)
}

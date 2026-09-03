plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.content.quest)
}

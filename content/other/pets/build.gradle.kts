plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.bottomlessMilk)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.quest)
}

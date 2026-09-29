plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spellsAutocast)
    implementation(projects.content.quest)
}

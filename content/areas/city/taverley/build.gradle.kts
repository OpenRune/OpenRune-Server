plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.quest)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
}

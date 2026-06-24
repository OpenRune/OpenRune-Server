plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
}

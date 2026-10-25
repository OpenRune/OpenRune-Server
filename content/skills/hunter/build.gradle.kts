plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.quest)
    implementation(projects.api.utils.utilsSkills)
}

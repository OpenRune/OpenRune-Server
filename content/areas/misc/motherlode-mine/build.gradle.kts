plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.api.utils.utilsSkills)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.skills.mining)
}

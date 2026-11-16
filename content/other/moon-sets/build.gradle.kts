plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.specials)
    implementation(projects.api.spells)
    implementation(projects.api.mechanics.burn)
    implementation(projects.api.weapons)
}

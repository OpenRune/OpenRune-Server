plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spellsAutocast)
    implementation(projects.api.spellsRunes)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.quest)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.toml)
    implementation(libs.jackson.module.kotlin)
}

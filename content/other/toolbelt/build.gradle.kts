plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.api.invStorage)
    implementation(projects.content.other.toolbelt.pack)
    implementation(projects.content.skills.utils)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.toml)
    implementation(libs.jackson.module.kotlin)
}

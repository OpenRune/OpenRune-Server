plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.content.generic.genericLocs)
    implementation(projects.content.interfaces.bank)
}

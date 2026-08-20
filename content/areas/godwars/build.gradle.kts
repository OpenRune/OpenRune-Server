plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.areaChecker)
    implementation(projects.api.instances)
    implementation(projects.api.pluginCommons)
    implementation(projects.engine.utilsBits)
}

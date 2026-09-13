plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.content.generic.killcount)
    implementation(projects.api.pluginCommons)
    implementation(projects.engine.utilsBits)
}

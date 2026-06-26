plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.instances)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.api.script)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
}

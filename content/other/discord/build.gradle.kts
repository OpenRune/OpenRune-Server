plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.net)
    implementation(projects.api.playerOutput)
    implementation(projects.api.pluginCommons)
    implementation(projects.engine.game)
    implementation(projects.engine.plugin)
}

plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.attr)
    implementation(projects.api.player)
    implementation(projects.api.registry)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.api.script)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.module)
    implementation(projects.engine.plugin)
}

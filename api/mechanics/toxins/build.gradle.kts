plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.config)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.script)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
    implementation(projects.engine.plugin)
}

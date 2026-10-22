plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
    id("integration-test-suite")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.config)
    implementation(projects.api.generated)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.repo)
    implementation(projects.api.script)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
    implementation(projects.engine.plugin)
    integrationImplementation(projects.api.death)
    integrationImplementation(projects.api.deathPlugin)
    integrationImplementation(projects.api.hitPlugin)
    integrationImplementation(projects.engine.module)
}

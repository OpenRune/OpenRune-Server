plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.kotlin.inline.logger)
    runtimeOnly(libs.logback.classic)
    implementation(libs.fastutil)
    implementation(libs.guice)
    implementation(projects.api.cache)
    implementation(projects.api.config)
    implementation(projects.api.gameProcess)
    implementation(projects.api.invtx)
    implementation(projects.api.market)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.script)

    implementation(projects.api.utils.utilsFormat)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
    implementation(projects.engine.objtx)
    implementation(projects.engine.plugin)
    testImplementation(projects.engine.annotations)
    testImplementation(projects.api.invStorage)
}

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
    implementation(libs.rsprot.api)
    implementation(projects.api.account)
    implementation(projects.api.config)
    implementation(projects.api.controller)
    implementation(projects.api.dbGateway)
    implementation(projects.api.hunt)
    implementation(projects.api.invWeight)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.random)
    implementation(projects.api.registry)
    implementation(projects.api.repo)
    implementation(projects.api.route)
    implementation(projects.api.stats.levelmod)
    implementation(projects.api.utils.utilsLogging)
    implementation(projects.api.utils.utilsMap)
    implementation(projects.api.utils.utilsZone)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
    implementation(projects.engine.interact)
    implementation(projects.engine.map)
    implementation(projects.engine.routefinder)

    testImplementation(projects.api.areaChecker)
    testImplementation(projects.api.market)
    testImplementation(projects.engine.coroutine)
}

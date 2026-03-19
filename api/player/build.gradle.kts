plugins {
    id("base-conventions")

}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.bundles.logging)
    implementation(libs.fastutil)
    implementation(libs.guice)
    implementation(libs.rsprot.api)
    implementation(projects.api.areaChecker)
    implementation(projects.api.config)
    implementation(projects.api.hunt)
    implementation(projects.api.invtx)
    implementation(projects.api.market)
    implementation(projects.api.music)
    implementation(projects.api.playerOutput)
    implementation(projects.api.random)
    implementation(projects.api.repo)
    implementation(projects.api.route)
    implementation(projects.api.tables)
    implementation(projects.api.stats.levelmod)


    implementation(projects.api.utils.utilsFormat)
    implementation(projects.api.utils.utilsMap)
    implementation(projects.api.utils.utilsSkills)
    implementation(projects.api.utils.utilsVars)
    implementation(projects.engine.annotations)
    implementation(projects.engine.coroutine)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.objtx)
    implementation(projects.engine.plugin)
    implementation(projects.engine.routefinder)
    implementation(projects.engine.utilsBits)
}

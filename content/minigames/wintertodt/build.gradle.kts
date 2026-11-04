plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.attr)
    implementation(projects.api.config)
    implementation(projects.api.death)
    implementation(projects.api.gameProcess)
    implementation(projects.api.invtx)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.random)
    implementation(projects.api.registry)
    implementation(projects.api.repo)
    implementation(projects.api.route)
    implementation(projects.api.stats.levelmod)
    implementation(projects.api.utils.utilsSkills)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.other.consumables)
}

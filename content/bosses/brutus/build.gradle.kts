plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    testImplementation(projects.api.invPlugin)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.registry)
    implementation(projects.api.areaChecker)
    implementation(projects.api.bosses)
    implementation(projects.api.bossHpBarPlugin)
    implementation(projects.api.db)
    implementation(projects.api.dbGateway)
    implementation(projects.api.death)
    implementation(projects.api.instances)
    implementation(projects.api.npc)
    implementation(projects.api.objCharges)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.random)
    implementation(projects.api.repo)
    implementation(projects.api.route)
    implementation(projects.content.quest)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.utilsBits)
}

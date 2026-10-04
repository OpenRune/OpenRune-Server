plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.bossHpBarPlugin)
    implementation(projects.api.bosses)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.config)
    implementation(projects.api.death)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.instances)
    implementation(projects.api.invtx)
    implementation(projects.api.market)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.api.mechanics.toxins)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.realm)
    implementation(projects.api.repo)
    implementation(projects.api.route)
    implementation(projects.api.pluginCommons)
    testImplementation(libs.fastutil)
}

plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.death)
    implementation(projects.api.instances)
    implementation(projects.api.invtx)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.playerOutput)
    implementation(projects.api.script)
    implementation(projects.api.utils.utilsFormat)
    implementation(projects.api.random)
    implementation(projects.api.repo)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
}

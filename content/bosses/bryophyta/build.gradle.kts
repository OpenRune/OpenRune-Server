plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.instances)
    implementation(projects.api.mechanics.toxins)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.api.route)
    implementation(projects.content.drops)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.content.skills.prayer)
    implementation(projects.content.skills.slayer)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    testImplementation(projects.api.testing.testRandom)
}

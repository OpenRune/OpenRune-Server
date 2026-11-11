plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.areaChecker)
    implementation(projects.api.bosses)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.combat.combatWeapon)
    implementation(projects.api.death)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.instances)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.random)
    implementation(projects.api.repo)
    implementation(projects.api.script)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.content.travel.mapTransports)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.plugin)
}

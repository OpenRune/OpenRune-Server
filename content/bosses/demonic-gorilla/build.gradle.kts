plugins {
    id("base-conventions")

}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.random)
    implementation(projects.api.script)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.plugin)
    implementation(projects.engine.coroutine)
}

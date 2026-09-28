plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.bosses)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.config)
    implementation(projects.api.death)
    implementation(projects.api.instances)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.route)
    implementation(projects.engine.game)
    implementation(projects.engine.plugin)
}

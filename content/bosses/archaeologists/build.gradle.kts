plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.config)
    implementation(projects.api.death)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.random)
    implementation(projects.api.repo)
    implementation(projects.api.script)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.plugin)
}

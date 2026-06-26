plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.random)
    implementation(projects.api.repo)
    implementation(projects.api.script)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.plugin)
    implementation(projects.engine.coroutine)
}

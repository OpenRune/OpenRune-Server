plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.bossHpBarPlugin)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.repo)
    implementation(projects.api.random)
    implementation(projects.api.registry)
}

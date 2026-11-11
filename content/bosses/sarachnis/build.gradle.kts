plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.areaChecker)
    implementation(projects.api.bosses)
    implementation(projects.api.config)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.player)
    implementation(projects.api.npc)
    implementation(projects.api.repo)
    implementation(projects.api.route)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.instances)
    implementation(projects.api.random)
}

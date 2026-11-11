plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.npc)
    implementation(projects.api.route)
}

plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.bossHpBarPlugin)
    implementation(projects.api.bosses)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.instances)
    implementation(projects.api.pluginCommons)
}

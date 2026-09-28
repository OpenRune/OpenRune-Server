plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.areas.wilderness)
}

plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.config)
    implementation(projects.api.death)
    implementation(projects.api.generated)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.script)
    implementation(projects.api.specials)
    implementation(projects.api.spells)
}

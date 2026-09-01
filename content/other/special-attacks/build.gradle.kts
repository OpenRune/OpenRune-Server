plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.fastutil)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatScripts)
    implementation(projects.api.combat.combatWeapon)
    implementation(projects.api.mechanics.toxins)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.specials)
    implementation(projects.engine.utilsBits)
}

plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.api.areaChecker)
    implementation(projects.api.bossHpBarPlugin)
    implementation(projects.api.bosses)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.registry)
    implementation(projects.api.mechanics.toxins)
    implementation(libs.rsprot.api)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.content.other.consumables)
}

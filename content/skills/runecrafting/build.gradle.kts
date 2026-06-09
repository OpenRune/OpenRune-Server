plugins {
    id("base-conventions")

}

dependencies {
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.invStorage)
    implementation(projects.api.invtx)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.api.script)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.spells)
    implementation(projects.api.stats.xpmod)
    implementation(projects.content.skills.utils)
}


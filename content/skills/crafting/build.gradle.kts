plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.attr)
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.script)
    implementation(projects.api.spells)
    implementation(projects.content.generic.genericLocs)
    implementation(projects.content.quest)
    implementation(projects.content.skills.utils)
}

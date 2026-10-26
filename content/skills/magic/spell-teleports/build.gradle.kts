plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.generated)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.script)
    implementation(projects.api.spells)
    implementation(projects.api.spellsRunes)
    implementation(projects.content.quest)
    integrationImplementation(projects.content.quest)
    integrationImplementation(projects.api.spells)
    integrationImplementation(projects.api.utils.utilsTime)
    integrationImplementation(projects.api.combat.combatCommons)
}

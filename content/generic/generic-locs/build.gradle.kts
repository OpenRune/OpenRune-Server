plugins {
    id("base-conventions")
    id("integration-test-suite")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.interfaces.bank)
    integrationImplementation(projects.content.interfaces.bank)
    integrationImplementation(projects.content.interfaces.equipment)
    integrationImplementation(projects.api.combat.combatWeapon)
    integrationImplementation(projects.api.death)
    integrationImplementation(projects.api.deathPlugin)
    integrationImplementation(projects.api.hitPlugin)
    testImplementation(projects.api.invStorage)
}

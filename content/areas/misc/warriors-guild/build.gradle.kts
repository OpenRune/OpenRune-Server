plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.areaChecker)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.npc)
    implementation(projects.content.drops)
    implementation(projects.api.combat.combatWeapon)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.death)
    implementation(projects.api.invtx)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.random)
    implementation(projects.api.repo)
    implementation(projects.api.shops)
    implementation(projects.content.generic.genericLocs)
    testImplementation(projects.api.invStorage)
    testImplementation(libs.mockk)
}

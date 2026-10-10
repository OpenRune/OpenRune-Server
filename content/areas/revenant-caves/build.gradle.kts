plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.dropTable)
    implementation(projects.api.attr)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.death)
    implementation(projects.api.invtx)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.weapons)
    implementation(projects.engine.utilsBits)
    implementation(projects.content.areas.wilderness)
    implementation(projects.content.skills.slayer)
    implementation(projects.content.interfaces.collectionLog)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.registry)
    testImplementation(projects.content.drops)
    testImplementation("org.mockito:mockito-core:5.14.2")
}

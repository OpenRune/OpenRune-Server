plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation(projects.api.invStorage)
    implementation(projects.engine.utilsBits)
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.api.weapons)
}

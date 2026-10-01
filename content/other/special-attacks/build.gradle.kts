plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    testImplementation(projects.api.invStorage)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.specials)
    implementation(projects.api.combat.combatFormulas)
    testImplementation("org.mockito:mockito-core:5.14.2")
}

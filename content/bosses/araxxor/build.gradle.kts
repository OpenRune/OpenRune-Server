plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    testImplementation(libs.or2.all.cache)
    testImplementation(libs.fastutil)
    testImplementation(projects.api.registry)
    testImplementation("org.mockito:mockito-core:5.14.2")
    implementation(projects.api.bosses)
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.instances)
    implementation(projects.api.pluginCommons)
}

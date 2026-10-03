plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    testImplementation(libs.or2.all.cache)
    testImplementation(projects.api.invStorage)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.specials)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.content.quest)
    testImplementation("org.mockito:mockito-core:5.14.2")
}

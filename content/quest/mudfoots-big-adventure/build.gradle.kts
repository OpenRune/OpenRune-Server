plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.death)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.content.other.pets)
    implementation(projects.content.quest)
    testImplementation(projects.api.invStorage)
}

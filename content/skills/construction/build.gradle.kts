plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.config)
    implementation(projects.api.instances)
    implementation(projects.api.invtx)
    implementation(projects.api.mechanics.toxins)
    implementation(projects.api.player)
    implementation(projects.api.registry)
    implementation(projects.api.repo)
    implementation(projects.api.spells)
    implementation(projects.content.quest)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.engine.events)
}

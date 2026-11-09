plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.script)
    implementation(projects.api.spells)
    implementation(projects.content.generic.genericLocs)
    implementation(projects.content.quest)
    implementation(projects.content.skills.utils)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.registry)
    testImplementation(projects.api.market)
    testImplementation(libs.guice)
}

plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.instances)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.combat.combatFormulas)
    implementation(libs.fastutil)
}

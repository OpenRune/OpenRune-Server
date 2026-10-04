plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    testImplementation(projects.api.invStorage)
    testImplementation(libs.rsprot.api)
    testImplementation("org.mockito:mockito-core:5.14.2")
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.script)
    implementation(projects.api.spells)
    implementation(projects.content.generic.genericLocs)
    implementation(projects.content.quest)
    implementation(projects.content.skills.utils)
}

plugins {
    id("base-conventions")
    id("game-cache-test-conventions")

}

dependencies {
    implementation(projects.content.interfaces.bank)
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.invStorage)
    implementation(projects.api.invtx)
    implementation(projects.api.death)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.api.script)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.spells)
    implementation(projects.api.stats.xpmod)
    implementation(projects.content.skills.utils)
    testImplementation(projects.api.net)
    testImplementation(libs.rsprot.api)
    testImplementation("org.mockito:mockito-core:5.14.2")
}


plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.attr)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.areaChecker)
    implementation(projects.api.config)
    implementation(projects.api.death)
    implementation(projects.api.generated)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.repo)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.api.route)
    implementation(projects.api.random)
    implementation(projects.api.script)
    implementation(projects.api.shops)
    implementation(projects.engine.game)
    implementation(projects.api.invtx)
    implementation(projects.content.skills.utils)

    integrationImplementation(projects.api.generated)
    integrationImplementation(projects.engine.game)
}

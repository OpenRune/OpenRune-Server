plugins {
    id("base-conventions")
    id("game-cache-test-conventions")

}

kotlin {
    explicitApi()
}

dependencies {
    testImplementation(libs.or2.all.cache)
    testImplementation(libs.fastutil)
    implementation(libs.guice)
    implementation(projects.api.combatAccuracy)
    implementation(projects.api.combatMaxhit)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatWeapon)
    implementation(projects.api.config)
    implementation(projects.api.areaChecker)
    implementation(projects.engine.utilsBits)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.random)
    implementation(projects.api.utils.utilsVars)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.module)
    implementation(projects.engine.plugin)
}

plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    testImplementation(projects.api.invStorage)
    implementation(libs.fastutil)
    implementation(libs.simmetrics.core)
    implementation(projects.api.areaChecker)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.instances)
    implementation(projects.api.registry)
    implementation(projects.api.db)
    implementation(projects.api.dbGateway)
    implementation(projects.api.mechanics.toxins)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spellsAutocast)

    implementation(projects.api.utils.utilsSystem)
    implementation(projects.engine.utilsBits)
    testImplementation(libs.rsprot.api)
    testImplementation("org.mockito:mockito-core:5.14.2")
}

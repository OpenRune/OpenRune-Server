plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.fastutil)
    implementation(libs.simmetrics.core)
    implementation(projects.api.areaChecker)
    implementation(projects.api.registry)
    implementation(projects.api.db)
    implementation(projects.api.dbGateway)
    implementation(projects.api.mechanics.toxins)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spellsAutocast)

    implementation(projects.api.utils.utilsSystem)
    implementation(projects.engine.utilsBits)
}

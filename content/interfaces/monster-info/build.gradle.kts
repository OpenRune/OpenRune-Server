plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.dropTable)
    implementation(projects.api.areaChecker)
    implementation(projects.api.combatMaxhit)
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation(libs.rsprot.api)
}

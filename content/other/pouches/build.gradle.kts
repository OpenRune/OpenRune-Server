plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.areaChecker)
    implementation(projects.api.attr)
    implementation(projects.api.invStorage)
    implementation(projects.api.death)
    implementation(projects.api.market)
    testImplementation(projects.api.net)
    testImplementation(projects.api.objPlugin)
    testImplementation(projects.api.registry)
    testImplementation(projects.content.skills.runecrafting)
    testImplementation(libs.rsprot.api)
    testImplementation(libs.fastutil)
    testImplementation("org.mockito:mockito-core:5.14.2")
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.content.interfaces.bank)
}

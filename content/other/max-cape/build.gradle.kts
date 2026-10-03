plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spellsAutocast)
    implementation(projects.content.other.consumables)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.net)
    testImplementation(libs.rsprot.api)
    testImplementation(libs.fastutil)
    testImplementation("org.mockito:mockito-core:5.14.2")
}

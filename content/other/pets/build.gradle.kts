plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.quest)
    testImplementation(projects.api.registry)
    testImplementation(libs.rsprot.api)
}

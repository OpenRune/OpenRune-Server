plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.api.shops)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.interfaces.collectionLog)
    testImplementation(projects.api.invPlugin)
    testImplementation(projects.api.gameProcess)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.registry)
    testImplementation(libs.fastutil)
}

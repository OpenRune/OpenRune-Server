plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.bottomlessMilk)
    implementation(projects.api.objCharges)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.interfaces.bank)

    testImplementation(projects.api.invPlugin)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.registry)
}

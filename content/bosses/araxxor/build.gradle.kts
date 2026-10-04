plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    testImplementation(libs.or2.all.cache)
    implementation(projects.api.bosses)
    implementation(projects.api.instances)
    implementation(projects.api.pluginCommons)
}

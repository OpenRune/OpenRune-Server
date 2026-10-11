plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}
dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.api.invStorage)
    implementation(projects.api.death)
    implementation(projects.api.scriptAdvanced)
    testImplementation(libs.fastutil)
    testImplementation(projects.api.objPlugin)
    testImplementation("org.mockito:mockito-core:5.14.2")
}

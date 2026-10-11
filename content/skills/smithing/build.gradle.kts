plugins {
    id("base-conventions")
    id("game-cache-test-conventions")

}

dependencies {
    implementation(projects.content.interfaces.bank)
    implementation(projects.api.invStorage)
    implementation(projects.api.death)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.content.skills.utils)
    implementation(projects.content.skills.crafting)
    testImplementation(projects.api.net)
    testImplementation(libs.rsprot.api)
    testImplementation("org.mockito:mockito-core:5.14.2")
}

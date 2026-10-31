plugins {
    id("base-conventions")
    id("game-cache-test-conventions")

}

dependencies {
    implementation(projects.api.attr)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.quest)
    implementation(projects.content.skills.utils)
    testImplementation(libs.mockk)
    testImplementation(libs.rsprot.api)
}

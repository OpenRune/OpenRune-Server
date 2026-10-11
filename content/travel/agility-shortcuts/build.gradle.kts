plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}
dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.invStorage)
    implementation(projects.api.stats.xpmod)
    implementation(projects.content.quest)
    implementation(projects.content.skills.mining)
    testImplementation(projects.content.areas.city.lumbridge)
    testImplementation(libs.fastutil)
    testImplementation("org.mockito:mockito-core:5.14.2")
}

plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}
dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.api.invStorage)
    implementation(projects.api.stats.xpmod)
    implementation(projects.content.quest)
    implementation(projects.content.skills.mining)
    testImplementation(projects.content.areas.city.lumbridge)
    testImplementation(projects.content.areas.city.portSarim)
    testImplementation(projects.content.areas.misc.motherlodeMine)
    testImplementation(libs.fastutil)
    testImplementation("org.mockito:mockito-core:5.14.2")
}

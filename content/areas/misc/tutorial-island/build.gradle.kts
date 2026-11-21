plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
    id("integration-test-suite")
}

tasks.named<Test>("test") {
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
    systemProperty("mockk.junit.extension.keepmocks", "true")
}

dependencies {
    implementation(projects.api.attr)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.spells)
    implementation(projects.content.quest)
    implementation(projects.content.skills.fishing)
    implementation(projects.content.skills.mining)
    implementation(projects.content.skills.smithing)
    implementation(projects.content.skills.woodcutting)
    testImplementation(projects.api.invtx)
    testImplementation(projects.api.stats.xpmod)
    testImplementation(projects.api.testing)
    testImplementation(projects.content.quest)
    testImplementation(projects.orCache)
}

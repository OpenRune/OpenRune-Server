plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    testImplementation(projects.api.invPlugin)
    testImplementation(projects.api.invStorage)
    testImplementation(projects.api.registry)
    testImplementation(libs.mockk)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spells)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.content.other.pets)
    implementation(projects.content.quest)
    implementation(projects.api.utils.utilsSkills)
    implementation(projects.content.skills.utils)
    implementation(projects.api.utils.utilsTime)
}

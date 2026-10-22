plugins {
    id("base-conventions")
    id("game-cache-test-conventions")

}

dependencies {
    implementation(projects.api.invStorage)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.content.skills.utils)
    implementation(projects.content.skills.crafting)
    implementation(projects.content.quest)
}

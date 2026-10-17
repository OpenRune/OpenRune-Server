plugins {
    id("base-conventions")

}

dependencies {
    implementation(projects.api.attr)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.quest)
    implementation(projects.content.skills.utils)
}

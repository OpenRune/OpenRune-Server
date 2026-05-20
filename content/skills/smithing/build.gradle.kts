plugins {
    id("base-conventions")

}

dependencies {
    implementation(projects.api.invStorage)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.content.skills.utils)
}

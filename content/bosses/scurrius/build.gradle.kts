plugins {
    id("base-conventions")

}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.instances)
    implementation(projects.api.route)
    implementation(projects.api.npc)
}

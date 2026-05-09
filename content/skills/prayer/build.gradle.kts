plugins {
    id("base-conventions")

}

dependencies {
    implementation(projects.api.areaChecker)
    implementation(projects.api.death)
    implementation(projects.api.player)
    implementation(projects.api.script)
    implementation(projects.api.objCharges)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.content.skills.utils)
    implementation(projects.engine.events)
    implementation(projects.engine.utilsBits)
}

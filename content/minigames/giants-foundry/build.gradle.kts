plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.attr)
    implementation(projects.api.config)
    implementation(projects.api.instances)
    implementation(projects.api.invtx)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.random)
    implementation(projects.api.repo)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.quest)
    implementation(projects.content.skills.utils)
}

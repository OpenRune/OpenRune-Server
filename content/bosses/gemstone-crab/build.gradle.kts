plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.npc)
    implementation(projects.api.repo)
    implementation(projects.api.bossHpBarPlugin)
    implementation(projects.api.random)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.content.skills.mining)
}

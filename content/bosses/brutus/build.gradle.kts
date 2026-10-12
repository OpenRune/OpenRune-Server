plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.bosses)
    implementation(projects.api.bossHpBarPlugin)
    implementation(projects.api.death)
    implementation(projects.api.instances)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.api.route)
    implementation(projects.content.quest)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
}

plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.areaChecker)
    implementation(projects.api.config)
    implementation(projects.api.death)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.player)
    implementation(projects.api.random)
    implementation(projects.api.repo)
    implementation(projects.content.skills.slayer)
    implementation(projects.content.quest)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.plugin)
}

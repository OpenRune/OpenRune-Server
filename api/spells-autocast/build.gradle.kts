plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.config)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)



    implementation(projects.engine.game)
    implementation(projects.engine.plugin)
}

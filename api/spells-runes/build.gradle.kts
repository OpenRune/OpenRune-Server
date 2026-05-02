plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.config)
    implementation(projects.api.generated)
    implementation(projects.api.combat.combatCommons)


    implementation(projects.engine.game)
    implementation(projects.engine.plugin)
    testImplementation(libs.fastutil)
}

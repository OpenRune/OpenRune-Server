plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(projects.api.objCharges)
    implementation(projects.api.player)
    implementation(projects.engine.game)
    implementation(projects.engine.objtx)
    implementation(projects.engine.utilsBits)
}

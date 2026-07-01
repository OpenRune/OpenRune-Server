plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.rsprot.api)
    implementation(projects.api.playerOutput)
    implementation(projects.engine.game)
}

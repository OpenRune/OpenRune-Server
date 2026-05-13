plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.guice)
    implementation(libs.fastutil)
    implementation(libs.rsprot.api)
    implementation(projects.api.account)
    implementation(projects.api.playerOutput)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.routefinder)
}

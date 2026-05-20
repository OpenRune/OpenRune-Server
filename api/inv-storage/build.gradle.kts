plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.guice)
    implementation(projects.engine.game)
    implementation(projects.engine.objtx)
    implementation(projects.engine.plugin)
}

plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.classgraph)
    implementation(libs.guice)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
}

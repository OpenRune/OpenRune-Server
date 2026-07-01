plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.fastutil)
    implementation(libs.openrune.central.common)
    implementation(projects.engine.annotations)
    implementation(projects.api.attr)
    implementation(projects.engine.coroutine)
    implementation(projects.engine.events)
    api(projects.engine.map)
    implementation(projects.engine.routefinder)
    implementation(projects.engine.utilsBits)
    implementation(projects.engine.utilsSorting)
}

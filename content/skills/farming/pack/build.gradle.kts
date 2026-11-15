plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.engine.map)
    implementation(libs.or2.all.cache)
}

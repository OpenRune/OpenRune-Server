plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.engine.map)
    implementation(libs.or2.all.cache)
    implementation(libs.or2.tools)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.toml)
}

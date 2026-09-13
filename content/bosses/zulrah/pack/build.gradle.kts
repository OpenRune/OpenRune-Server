plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.or2.all.cache)
    implementation(libs.or2.tools)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.dataformat.toml)
}

tasks.test {
    workingDir = rootProject.projectDir
}

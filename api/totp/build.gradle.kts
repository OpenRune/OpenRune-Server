plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.apache.commons)
    implementation(libs.bundles.logging)
    implementation(libs.guice)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.otp.kotlin)
    implementation(projects.engine.module)
}

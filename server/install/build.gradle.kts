plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.kotlin.inline.logger)
    runtimeOnly(libs.logback.classic)
    implementation(libs.clikt)
    implementation(libs.guice)
    implementation(libs.okhttp)
    implementation(libs.openrs2.cache)
}

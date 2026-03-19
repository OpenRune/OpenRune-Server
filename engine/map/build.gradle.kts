plugins {
    id("base-conventions")
    id("meta-test-suite")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.fastutil)
}

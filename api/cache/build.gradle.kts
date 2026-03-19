plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    // Source: https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.13.2")
    // Source: https://mvnrepository.com/artifact/cc.ekblad/4koma
    implementation("cc.ekblad:4koma:1.2.0")

    implementation(libs.kotlin.coroutines.core)
    implementation(libs.fastutil)
    implementation(libs.openrs2.buffer)
    implementation(libs.openrs2.cache)
    implementation(projects.api.repo)
    implementation(projects.engine.annotations)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.module)
    implementation(projects.engine.routefinder)
}

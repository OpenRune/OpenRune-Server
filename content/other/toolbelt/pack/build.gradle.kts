plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.orCache.packApi)
    implementation(libs.or2.all.cache)
    implementation(libs.or2.tools)
    implementation(libs.or2.definition)
    implementation(libs.or2.filestore)
    implementation(libs.or2.filesystem)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.toml)
    implementation("io.netty:netty-buffer:4.1.107.Final")
    implementation("me.tongfei:progressbar:0.9.2")
    implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger:1.0.6")
}

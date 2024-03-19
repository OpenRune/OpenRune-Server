
description = "OpenRune Cache"

val lib = rootProject.project.libs
dependencies {

    implementation("com.displee:rs-cache-library:${findProperty("displeeCacheVersion")}")

    implementation("com.github.jponge:lzma-java:1.3")
    implementation("it.unimi.dsi:fastutil:${findProperty("fastUtilVersion")}")
    implementation("ch.qos.logback:logback-classic:${findProperty("logbackVersion")}")
    implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger-jvm:${findProperty("inlineLoggingVersion")}")
}

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.shadow)
    id("base-conventions")
    application
}

application {
    mainClass.set("org.rsmod.server.app.GameServerKt")
    // Without an explicit -Xmx the JVM takes 1/4 of host RAM, and G1 keeps whatever it expanded to
    // during the cache-decoding burst at boot. The free-ratio bounds plus periodic concurrent GC
    // let it hand that memory back once the server settles into its idle working set.
    applicationDefaultJvmArgs =
        listOf(
            "-XX:AutoBoxCacheMax=65535",
            "-Xms512m",
            "-Xmx4g",
            "-XX:MinHeapFreeRatio=5",
            "-XX:MaxHeapFreeRatio=20",
            "-XX:+G1PeriodicGCInvokesConcurrent",
            "-XX:G1PeriodicGCInterval=15000",
        )
}

dependencies {
    implementation(libs.kotlin.inline.logger)
    runtimeOnly(libs.logback.classic)
    implementation(libs.clikt)
    implementation(libs.guice)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.openrs2.cache)
    implementation(projects.api.cache)
    implementation(projects.api.core)
    implementation(projects.api.gameProcess)
    implementation(projects.api.mechanics.toxins)
    implementation(projects.api.invPlugin)
    implementation(projects.api.net)
    implementation(projects.api.objPlugin)
    implementation(projects.api.parsers.jackson)
    implementation(projects.api.parsers.json)
    implementation(projects.api.parsers.toml)
    implementation(projects.api.registry)
    implementation(projects.api.shops)
    implementation(projects.engine.annotations)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.module)
    implementation(projects.engine.routefinder)
    implementation(projects.engine.plugin)
    implementation(projects.server.install)
    implementation(projects.server.logging)
    implementation(projects.server.services)
    implementation(projects.server.shared)
}

tasks.named<JavaExec>("run") {
    description = "Runs the RS Mod game server"
    workingDir = rootProject.projectDir
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("server.jar")
    mergeServiceFiles()
    isZip64 = true
}

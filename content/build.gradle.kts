import org.jetbrains.kotlin.gradle.plugin.mpp.external.project
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.ksp)
}

description = "Server Content"

dependencies {
    implementation(projects.util)
    implementation(projects.cache)
    implementation(project(":game-api"))
    api(project(":game-server"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation(rootProject.project.libs.rsprot)
    implementation(rootProject.project.libs.routefinder)
    implementation(rootProject.project.libs.jackson.module.kotlin)
    ksp(project(":ksp-processor"))
}

tasks.withType<KotlinCompile> {
    dependsOn(":game-server:build")
}


ksp {
    arg("moduleDir", projectDir.absolutePath)
    arg("rscmDir", rootDir.absolutePath + "/data/cfg/rscm2/")
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
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
    ksp(project(":ksp-processor"))
}

tasks.withType<KotlinCompile> {
    dependsOn(":game-server:build")
}


ksp {
    arg("moduleDir", projectDir.absolutePath)
    arg("rootDir", rootDir.absolutePath + "/")
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
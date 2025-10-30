import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Server Content"

dependencies {
    implementation(projects.util)
    implementation(project(":game-api"))
    api(project(":game-server"))
    implementation(kotlin("script-runtime"))
    implementation(kotlin("scripting-jvm"))
    implementation(kotlin("scripting-jvm-host"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation(rootProject.project.libs.rsprot)
}

tasks.withType<KotlinCompile> {
    dependsOn(":game-server:build")
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
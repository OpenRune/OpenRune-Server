import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

val Project.isUnderTools: Boolean
    get() = path.startsWith(":tools")

plugins.withType<JavaPlugin> {
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        withJavadocJar()
        withSourcesJar()
    }
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(
            if (project.isUnderTools) {
                JvmTarget.JVM_17
            } else {
                JvmTarget.JVM_21
            },
        )
        optIn = listOf("kotlin.contracts.ExperimentalContracts")
        freeCompilerArgs = listOf("-Xnested-type-aliases")
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    if (project.isUnderTools) {
        // Kotlin emits JVM 17; Java extension stays 21 so project deps (e.g. :or-cache) resolve.
        jvmTargetValidationMode.set(JvmTargetValidationMode.IGNORE)
    }
}

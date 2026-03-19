plugins {
    id("base-conventions")
}

dependencies {
    implementation(rootProject.libs.or2.all.cache)
    implementation(rootProject.project.libs.or2.tools)
    implementation(rootProject.project.libs.or2.server.utils)
    api(libs.or2.definition)
    api(libs.or2.filestore)
    api(libs.or2.filesystem)
    implementation("cc.ekblad:4koma:1.2.2-openrune")
    implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger:1.0.6")
    implementation("com.squareup:kotlinpoet:2.2.0")
    implementation("me.tongfei:progressbar:0.9.2")
    implementation(libs.jackson.dataformat.toml)
    implementation(libs.jackson.databind)
}

tasks {
    register("buildCache",JavaExec::class) {
        group = "cache"
        description = "Build Cache"
        dependsOn("checkCachePrerequisites")
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("dev.openrune.CacheToolsKt")
        args = listOf("BUILD")
    }

    register("freshCache",JavaExec::class) {
        group = "cache"
        description = "Fresh Install Cache"

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("dev.openrune.CacheToolsKt")
        args = listOf("FRESH_INSTALL")
    }

}

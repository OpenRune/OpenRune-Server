dependencies {
    implementation(rootProject.project.libs.or2.all.cache)
    implementation(rootProject.project.libs.or2.tools)
    implementation(rootProject.project.libs.or2.server.utils)
    api(libs.or2.definition)
    api(libs.or2.filestore)
    api(libs.or2.filesystem)

    implementation("com.squareup:kotlinpoet:2.2.0")
}

tasks {
    register("buildCache",JavaExec::class) {
        group = "cache"
        description = "Build Cache"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("org.alter.CacheToolsKt")
        args = listOf("BUILD")
    }

    register("freshCache",JavaExec::class) {
        group = "cache"
        description = "Fresh Install Cache"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("org.alter.CacheToolsKt")
        args = listOf("FRESH_INSTALL")
    }

}
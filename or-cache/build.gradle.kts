plugins {
    id("base-conventions")
}

dependencies {
    // Source: https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.13.2")
    implementation(libs.kotlin.coroutines.core)

    implementation(rootProject.libs.or2.all.cache)
    implementation(rootProject.project.libs.or2.tools)
    implementation(rootProject.project.libs.or2.server.utils)
    api(libs.or2.definition)
    api(libs.or2.filestore)
    api(libs.or2.filesystem)

    findContentPlugins().forEach { runtimeOnly(it) }
    implementation(projects.engine.map)
    implementation(projects.engine.routefinder)
    implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger:1.0.6")
    implementation("com.squareup:kotlinpoet:2.2.0")
    implementation("me.tongfei:progressbar:0.9.2")
    implementation("io.netty:netty-buffer:4.1.107.Final")
    implementation(libs.jackson.dataformat.toml)
    implementation(libs.jackson.databind)
    implementation("dev.or2:toml-rsconfig:1.0")
    compileOnly(libs.logback.classic)
    implementation(libs.fastutil)
    implementation(libs.classgraph)
}

// Only dedicated `pack` submodules are on the cache-build classpath. A content plugin keeps its
// cache data (db tables, configs, models, sprites) in a `<plugin>/pack` module so building the
// cache never has to compile the plugin's game scripts or their api/content dependencies.
fun findContentPlugins(): List<Project> =
    project(":content").subprojects.filter { it.name.endsWith("-pack") && it.buildFile.exists() }

tasks {
    register("buildCache",JavaExec::class) {
        group = "cache"
        description = "Build Cache"
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

    register<JavaExec>("exportBaseGamevals") {
        group = "cache"
        description = "Download the OSRS cache and export only its revision-specific gameval symbols"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("dev.openrune.CacheToolsKt")
        args = listOf("EXPORT_BASE_GAMEVALS")
    }

    register("cleanCs2", JavaExec::class) {
        group = "tools"
        description = "Deletes the generated CS2 directory from user app data."

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("dev.openrune.CacheToolsKt")
        args = listOf("CLEAN_CS2")
    }

    register<JavaExec>("downloadTransports") {
        group = "cache"
        description = "Updates the bundled Shortest Path TSVs using the current revision's gamevals"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("dev.openrune.CacheToolsKt")
        args = buildList {
            add("DOWNLOAD_TRANSPORTS")
            providers.gradleProperty("baseGamevalsDir").orNull?.let {
                add(rootProject.file(it).absolutePath)
            }
        }
    }

    register<JavaExec>("dumpLocs") {
        group = "tools"
        description = "Prints every placement of the locs in -Plocs (symbols, ids or x1:z1:x2:z2)"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("dev.openrune.CacheToolsKt")
        args = listOf("DUMP_LOCS", (project.findProperty("locs") ?: "").toString())
    }

    register<JavaExec>("dumpSeqs") {
        group = "tools"
        description = "Prints how many ticks the animations in -Pseqs run for"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("dev.openrune.CacheToolsKt")
        args = listOf("DUMP_SEQS", (project.findProperty("seqs") ?: "").toString())
    }

    register<JavaExec>("checkGamevals") {
        group = "tools"
        description =
            "Reports every \"table.name\" gameval referenced in source or TOML that no gamevals table " +
                "defines (-PfailOnMissing exits non-zero for CI)"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("dev.openrune.CacheToolsKt")
        args = buildList {
            add("CHECK_GAMEVALS")
            if (project.hasProperty("failOnMissing")) add("--fail")
            providers.gradleProperty("baseGamevalsDir").orNull?.let {
                add("--snapshot-dir=${rootProject.file(it).absolutePath}")
            }
        }
    }

    register<JavaExec>("mergePluginGamevals") {
        group = "cache"
        description =
            "Writes plugin gamevals.toml entries into .data/gamevals/*.rscm (used by release CI)"

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("dev.openrune.gamevals.PluginGamevalMergerKt")
        workingDir = rootProject.projectDir
        dependsOn("classes")
    }



}

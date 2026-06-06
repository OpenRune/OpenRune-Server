plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.orCache)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.api.parsers.json)
    implementation(projects.api.parsers.toml)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.or2.wiki)
    implementation("io.ktor:ktor-client-cio:3.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

tasks.register<JavaExec>("reformatDropTables") {
    group = "application"
    description = "Reformat drop table TOML files under content/drops resources."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.rsmod.tools.wiki.dumping.DropTableTomlReformatterKt")
    args(rootProject.file("content/drops/src/main/resources/drops/tables").absolutePath)
}

tasks.register<JavaExec>("dumpNpcDrops") {
    group = "application"
    description =
        "Runs NpcDropTableWikiDumper. Example: " +
            "./gradlew :tools:wiki-dumping:dumpNpcDrops --args=\"Black Knight --json --quiet\""
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.rsmod.tools.wiki.dumping.NpcDropTableWikiDumperKt")
}

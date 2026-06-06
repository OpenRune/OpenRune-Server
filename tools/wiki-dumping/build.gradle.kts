plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.orCache)
    implementation(projects.api.dropTable)
    implementation(libs.or2.wiki)
    implementation("io.ktor:ktor-client-cio:3.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

tasks.register<JavaExec>("dumpNpcDrops") {
    group = "application"
    description =
        "Runs NpcDropTableWikiDumper. Example: " +
            "./gradlew :tools:wiki-dumping:dumpNpcDrops --args=\"Black Knight --quiet\""
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.rsmod.tools.wiki.dumping.NpcDropTableWikiDumperKt")
}

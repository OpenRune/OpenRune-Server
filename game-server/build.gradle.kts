plugins {
    alias(libs.plugins.shadow)
    application
    `maven-publish`
}
description = "Alter Game Server Launcher"
application {
    apply(plugin = "maven-publish")
    mainClass.set("org.alter.game.Launcher")
}
val ktorVersion = "2.3.7"
val lib = rootProject.project.libs
dependencies {
    implementation(project(":cache"))



    with(lib) {
        implementation(projects.util)
        runtimeOnly(projects.gamePlugins)
        implementation(kotlinx.coroutines)
        implementation(reflection)
        implementation(commons)
        implementation(kotlin.scripting)
        implementation(kotlin.script.runtime)
        implementation(classgraph)
        implementation(fastutil)
        implementation(bouncycastle)
        implementation(jackson.module.kotlin)
        implementation(jackson.dataformat.yaml)
        implementation(kotlin.csv)
        implementation(mongo.bson)
        testRuntimeOnly(junit)
        implementation(rootProject.project.libs.rsprot)
        implementation(lib.routefinder)
        implementation(or2.server.utils)
        implementation(joda.time)
        implementation(or2.definition)
        implementation(or2.filesystem)
        implementation(or2.filestore)
        implementation(or2.central.api.client)
        implementation(lib.or2.central.server)
        implementation(or2.filestore)
    }
    implementation("io.ktor:ktor-server-core:${ktorVersion}")
    implementation("io.ktor:ktor-server-netty:${ktorVersion}")
}
sourceSets {
    named("main") {
        kotlin.srcDirs("src/main/kotlin")
        resources.srcDirs("src/main/resources")
    }
}

@Suppress("ktlint:standard:max-line-length")
tasks.register("installConfigs") {
    description = "Copy example configs into place"

    // IMPORTANT: don't use a Copy task writing to the repo root, because Gradle will treat the
    // whole directory (including build/ outputs) as this task's outputs and fail validation.
    val gameYml = file("${rootProject.projectDir}/game.yml")
    val devSettingsYml = file("${rootProject.projectDir}/dev-settings.yml")
    val centralServerYml = file("${rootProject.projectDir}/central-server.yml")
    val firstLaunch = file("${rootProject.projectDir}/first-launch")

    outputs.files(gameYml, devSettingsYml, centralServerYml, firstLaunch)

    doLast {
        project.copy {
            from("${rootProject.projectDir}/examples/game.example.yml")
            from("${rootProject.projectDir}/examples/dev-settings.example.yml")
            from("${rootProject.projectDir}/examples/central-server.example.yml")
            into("${rootProject.projectDir}/")

            rename("game.example.yml", "game.yml")
            rename("dev-settings.example.yml", "dev-settings.yml")
            rename("central-server.example.yml", "central-server.yml")
        }

        firstLaunch.createNewFile()
    }
}

tasks.register<JavaExec>("generateWorldKeys") {
    group = "application"
    description = "Generate Ed25519 keys and write them into central-server.yml and game.yml"

    dependsOn("installConfigs")

    workingDir = rootProject.projectDir
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.openrune.central.tools.KeyGenKt")
    args = listOf(
        "${rootProject.projectDir}/central-server.yml",
        "${rootProject.projectDir}/game.yml",
    )
}

tasks.register("install") {
    description = "Install Alter"

    dependsOn("runRsaService")
    dependsOn("installConfigs")
    dependsOn("generateWorldKeys")
    dependsOn(":cache:freshCache")
}

tasks.register<JavaExec>("runRsaService") {
    group = "application"
    workingDir = rootProject.projectDir
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.alter.game.service.rsa.RsaService")
    args = listOf("16", "1024", "./data/rsa/key.pem") // radix, bitcount, rsa pem file
}

task<Copy>("extractDependencies") {
    from(zipTree("build/distributions/game-server-${project.version}.zip")) {
        include("game-${project.version}/lib/*")
        eachFile {
            path = name
        }
        includeEmptyDirs = false
    }
    into("build/deps")
}

tasks.register<Copy>("applicationDistribution") {
    from("$rootDir/data/") {
        into("bin/data/")
        include("**")
        exclude("saves/*")
    }
}
tasks.named<Copy>("applicationDistribution") {
    from("$rootDir") {
        into("bin")
        include("/game-plugins/*")
        include("examples/game.example.yml")
        rename("game.example.yml", "game.yml")
    }
}


tasks.named("build") {
    finalizedBy("extractDependencies")
}
tasks.named("install") {
    dependsOn("build")
}
tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}


tasks.register("checkPrerequisites") {
    description = "Check if required files exist"
    doFirst {
        val gameYml = file("${rootProject.projectDir}/game.yml")
        val gameExampleYml = file("${rootProject.projectDir}/examples/game.example.yml")
        val devSettingsYml = file("${rootProject.projectDir}/dev-settings.yml")
        val cacheDir = file("${rootProject.projectDir}/data/cache")
        val rsaKey = file("${rootProject.projectDir}/data/rsa/key.pem")
        val rsaDir = file("${rootProject.projectDir}/data/rsa")
        
        val missingFiles = mutableListOf<String>()
        
        if (!gameYml.exists() && !gameExampleYml.exists()) {
            missingFiles.add("game.yml or examples/game.example.yml")
        }
        if (!devSettingsYml.exists()) {
            missingFiles.add("dev-settings.yml")
        }
        if (!cacheDir.exists() || !cacheDir.isDirectory || cacheDir.listFiles()?.isEmpty() != false) {
            missingFiles.add("data/cache (directory missing or empty)")
        }
        if (!rsaDir.exists() || !rsaDir.isDirectory) {
            missingFiles.add("data/rsa (directory missing)")
        }
        if (!rsaKey.exists()) {
            missingFiles.add("data/rsa/key.pem")
        }
        
        if (missingFiles.isNotEmpty()) {
            throw GradleException("Please run 'Install Server' first to set up the project.")
        }
    }
}

tasks.named<JavaExec>("run") {
    group = "application"

    // Ensure content is built before running
    dependsOn(":content:build")
    dependsOn("checkPrerequisites")

    // Optional: print message if content is being built
    doFirst {
        if (!project(":content").tasks.named("build").get().didWork) {
            println("Content is up-to-date, skipping build.")
        } else {
            println("Building content because it's out of date...")
        }
    }
}

/**
 * @TODO Forgot about this one.
 */
publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        groupId = "org.alter"
        artifactId = "alter"
        pom {
            packaging = "jar"
            name.set("Alter")
            description.set("AlterServer All")
        }
    }
}
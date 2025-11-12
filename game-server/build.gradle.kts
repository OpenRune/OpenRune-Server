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
        implementation(mongo.driver)
        testRuntimeOnly(junit)
        implementation(rootProject.project.libs.rsprot)
        implementation(lib.routefinder)
        implementation(or2.server.utils)
        implementation(joda.time)
        implementation(or2.definition)
        implementation(or2.filesystem)
        implementation(or2.filestore)
    }
}
sourceSets {
    named("main") {
        kotlin.srcDirs("src/main/kotlin")
        resources.srcDirs("src/main/resources")
    }
}

@Suppress("ktlint:standard:max-line-length")
tasks.register("install") {
    description = "Install Alter"

    dependsOn("runRsaService")
    dependsOn(":cache:freshCache")

    doLast {
        copy {
            into("${rootProject.projectDir}/")
            from("${rootProject.projectDir}/game.example.yml") {
                rename("game.example.yml", "game.yml")
            }
            from("${rootProject.projectDir}/dev-settings.example.yml") {
                rename("dev-settings.example.yml", "dev-settings.yml")
            }
            file("${rootProject.projectDir}/first-launch").createNewFile()
        }
    }
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
        include("game.example.yml")
        rename("game.example.yml", "game.yml")
    }
}
tasks.named<Zip>("shadowDistZip") {
    from("$rootDir/data/") {
        into("game-shadow-${project.version}/bin/data/")
        include("**")
        exclude("saves/*")
    }
    from("$rootDir") {
        into("game-shadow-${project.version}/bin/")
        include("/game-plugins/*")
        include("game.example.yml")
        rename("game.example.yml", "game.yml")
    }
}
tasks.register<Tar>("myShadowDistTar") {
    archiveFileName.set("game-shadow-${project.version}.tar")
    destinationDirectory.set(file("build/distributions/"))
    from("$rootDir/data/") {
        into("game-shadow-${project.version}/bin/data/")
        include("**")
        exclude("saves/*")
    }
    from("$rootDir") {
        into("game-shadow-${project.version}/bin/")
        include("/game-plugins/*")
        include("game.example.yml")
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


tasks.named<JavaExec>("run") {
    group = "application"

    // Ensure content is built before running
    dependsOn(":content:build")

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
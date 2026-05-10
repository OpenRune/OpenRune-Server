import java.nio.file.Files
import java.nio.file.Path

rootProject.name = "OpenRune-Server"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")
        maven("https://repo.openrs2.org/repository/openrs2-snapshots")
        maven("https://raw.githubusercontent.com/OpenRune/hosting/master")
    }
}

include(
    "api",
    "content",
    "engine",
    "server",
    "or-cache",
    "tools:osrs-mcp",
)

includeProjects(project(":api"))
includeProjects(project(":content"))
includeProjects(project(":engine"))
includeProjects(project(":server"))

fun includeProjects(pluginProject: ProjectDescriptor) {
    val projectPath = pluginProject.projectDir.toPath()
    Files.walk(projectPath).forEach {
        if (!Files.isDirectory(it)) {
            return@forEach
        }
        searchProject(pluginProject.name, projectPath, it)
    }
}

fun searchProject(parentName: String, root: Path, currentPath: Path) {
    val hasBuildFile = Files.exists(currentPath.resolve("build.gradle.kts"))
    if (!hasBuildFile) {
        return
    }
    val relativePath = root.relativize(currentPath)
    if (relativePath.toString().isEmpty()) {
        return
    }
    val projectName = relativePath.toString().replace(File.separator, ":")
    include("$parentName:$projectName")
}

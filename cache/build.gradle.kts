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
    register("checkCachePrerequisites") {
        description = "Check if required files exist for cache tasks"
        doFirst {
            val gameYml = file("${rootProject.projectDir}/game.yml")
            val gameExampleYml = file("${rootProject.projectDir}/game.example.yml")
            val cacheDir = file("${rootProject.projectDir}/data/cache")
            
            val missingFiles = mutableListOf<String>()
            
            if (!gameYml.exists() && !gameExampleYml.exists()) {
                missingFiles.add("game.yml or game.example.yml")
            }
            if (!cacheDir.exists() || !cacheDir.isDirectory || cacheDir.listFiles()?.isEmpty() != false) {
                missingFiles.add("data/cache (directory missing or empty)")
            }
            
            if (missingFiles.isNotEmpty()) {
                throw GradleException("Please run 'Install Server' first to set up the project.")
            }
        }
    }

    register("buildCache",JavaExec::class) {
        group = "cache"
        description = "Build Cache"
        dependsOn("checkCachePrerequisites")
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("org.alter.CacheToolsKt")
        args = listOf("BUILD")
    }

    register("freshCache",JavaExec::class) {
        group = "cache"
        description = "Fresh Install Cache"
        
        doFirst {
            // Only check prerequisites if not running as part of install task
            val installTask = project(":game-server").tasks.findByName("install")
            val isRunningAsPartOfInstall = installTask != null && 
                gradle.taskGraph.hasTask(installTask!!)
            
            if (!isRunningAsPartOfInstall) {
                val gameYml = file("${rootProject.projectDir}/game.yml")
                val gameExampleYml = file("${rootProject.projectDir}/game.example.yml")
                val cacheDir = file("${rootProject.projectDir}/data/cache")
                
                val missingFiles = mutableListOf<String>()
                
                if (!gameYml.exists() && !gameExampleYml.exists()) {
                    missingFiles.add("game.yml or game.example.yml")
                }
                if (!cacheDir.exists() || !cacheDir.isDirectory || cacheDir.listFiles()?.isEmpty() != false) {
                    missingFiles.add("data/cache (directory missing or empty)")
                }
                
                if (missingFiles.isNotEmpty()) {
                    throw GradleException("Please run 'Install Server' first to set up the project.")
                }
            }
        }
        
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("org.alter.CacheToolsKt")
        args = listOf("FRESH_INSTALL")
    }

}
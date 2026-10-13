plugins { id("base-conventions") }
dependencies {
    implementation("com.google.code.gson:gson:2.13.2")
    implementation(libs.or2.all.cache)
    implementation(libs.or2.tools)
    implementation(libs.or2.definition)
    implementation("io.netty:netty-buffer:4.1.107.Final")
}


tasks.register<JavaExec>("packCompletionistAssets") {
    group = "cache"
    description = "Rebuilds the completionist assets in this checkout's existing revision-240 cache."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.rsmod.content.other.nerocurator.pack.CompletionistPackTool")
    workingDir = rootProject.file("or-cache")
    args(rootProject.projectDir.absolutePath)
}

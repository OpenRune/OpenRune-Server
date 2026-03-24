val javafxVersion = "21"
val os = org.gradle.internal.os.OperatingSystem.current()
val platform = when {
    os.isWindows -> "win"
    os.isMacOsX -> "mac"
    else -> "linux"
}

dependencies {
    implementation(project(":cache"))
    implementation("org.openjfx:javafx-base:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$platform")
}

tasks.register<JavaExec>("runEditor") {
    group = "editor"
    description = "Launch the Map Editor"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.alter.editor.MapEditorAppKt")
}

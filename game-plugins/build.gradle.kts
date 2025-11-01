description = "Alter Servers Plugins"
val lib = rootProject.project.libs

dependencies {
    implementation(project(":cache"))
    api(project(":game-server"))
    implementation(lib.or2.definition)
    implementation(lib.or2.filestore)
    implementation(lib.or2.filesystem)
    implementation(lib.or2.server.utils)
    api(project(":util"))
    api(project(":game-api"))
    implementation(rootProject.project.libs.rsprot)
    implementation(lib.routefinder)
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    implementation(projects.api.delivery)
    implementation(projects.api.dropTable)
    implementation(projects.api.dropTablePlugin)
    implementation(projects.content.skills.mining)
    implementation(projects.content.skills.utils)
    testImplementation(libs.fastutil)
    testImplementation(libs.rsprot.api)
    testImplementation(projects.api.registry)
    testImplementation(projects.api.invStorage)
}

tasks.test {
    workingDir(rootProject.projectDir)
    inputs.dir(rootProject.file(".data/cache/SERVER"))
    inputs.file(rootProject.file(".data/gamevals-binary/gamevals.dat"))
}

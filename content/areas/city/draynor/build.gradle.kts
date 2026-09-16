plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.attr)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.quest)
    testImplementation(projects.content.generic.genericLocs)
    testImplementation(projects.api.hunt)
    testImplementation(libs.fastutil)
    testImplementation(projects.api.registry)
    testImplementation(projects.api.invStorage)
}

tasks.test {
    workingDir(rootProject.projectDir)
    inputs.dir(rootProject.file(".data/cache/SERVER"))
    inputs.file(rootProject.file(".data/gamevals-binary/gamevals.dat"))
}

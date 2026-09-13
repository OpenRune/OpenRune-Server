plugins { id("base-conventions") }

dependencies {
    implementation(projects.content.generic.killcount)
    implementation(projects.api.bosses)
    implementation(projects.api.instances)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.quest)
    implementation(projects.api.combat.combatFormulas)
    implementation(libs.jackson.module.kotlin)
    testImplementation(projects.engine.map)
    testImplementation(projects.engine.routefinder)
    testImplementation(projects.engine.events)
    testImplementation(projects.api.registry)
    testImplementation(projects.api.repo)
    testImplementation(projects.api.attr)
    testImplementation(projects.api.invStorage)
    testImplementation(libs.rsprot.api)
    testImplementation(libs.fastutil)
}

tasks.test {
    workingDir = rootProject.projectDir
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
}

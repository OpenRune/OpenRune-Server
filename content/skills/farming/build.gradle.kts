plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.content.interfaces.bank)
    implementation(projects.api.instances)
    implementation(projects.api.spells)
    implementation(projects.content.generic.genericLocs)
    implementation(projects.content.other.pets)
    implementation(projects.content.quest)
    implementation(projects.content.interfaces.omnishop)
    implementation(projects.content.interfaces.collectionLog)
    testImplementation(projects.api.invStorage)
    testImplementation(libs.fastutil)
    testImplementation(libs.rsprot.api)
}

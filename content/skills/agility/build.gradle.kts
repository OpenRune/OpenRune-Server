plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.content.quest)
    implementation(projects.api.shops)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.content.interfaces.omnishop)
    implementation(projects.content.other.pets)
    implementation(projects.content.other.lootingBag)
    integrationImplementation(projects.api.generated)
    integrationImplementation(projects.api.hitPlugin)
    integrationImplementation(projects.api.objPlugin)
    integrationImplementation(projects.api.shops)
    integrationImplementation(projects.content.quest)
    integrationImplementation(projects.content.other.lootingBag)
    integrationImplementation(projects.content.travel.mapTransports)
    integrationImplementation(projects.engine.coroutine)
    integrationImplementation(libs.rsprot.api)
}

plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.account)
    implementation(projects.api.db)
    implementation(projects.api.dbGateway)
    implementation(projects.api.generated)
    implementation(projects.api.pluginCommons)
    implementation(projects.server.services)
    implementation(libs.rsprot.api)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    integrationImplementation(projects.api.pluginCommons)
}

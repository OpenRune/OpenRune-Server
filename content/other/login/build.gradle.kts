plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.rsprot.api)
    implementation(projects.api.invWeight)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.realm)
    implementation(projects.api.net)
    implementation(projects.api.dbGateway)
    implementation(projects.api.social)
}

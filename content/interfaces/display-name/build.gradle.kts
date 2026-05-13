plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.db)
    implementation(projects.api.dbGateway)
    implementation(projects.api.social)
    implementation(libs.guice)
}

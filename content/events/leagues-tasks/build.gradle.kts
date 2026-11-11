plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.events.leaguesTasks.leaguesTasksPack)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.content.interfaces.journalTab)
    implementation(projects.content.interfaces.screenHighlight)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.toml)
    implementation(libs.jackson.module.kotlin)
}

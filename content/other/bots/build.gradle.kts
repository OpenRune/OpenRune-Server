plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.fastutil)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.net)
    implementation(projects.api.registry)
    implementation(projects.api.utils.utilsMap)
}

tasks.register<GenerateBotNames>("generateBotNames") {
    group = "tools"
    description = "Refreshes the cosmetic bot name TSV from the genr8rs OSRS name generator."
    outputFile.set(layout.projectDirectory.file("src/main/resources/bot-names.tsv"))
}

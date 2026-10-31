plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spells)
    implementation(projects.content.interfaces.collectionLog)
    implementation(projects.content.other.pets)
    implementation(projects.content.quest)
    implementation(projects.api.utils.utilsSkills)
    implementation(projects.content.skills.utils)
    implementation(projects.api.utils.utilsTime)
}

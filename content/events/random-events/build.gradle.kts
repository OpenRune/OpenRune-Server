plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.events.shootingStars)
    implementation(projects.content.quest)
}

plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.invStorage)
    implementation(projects.api.death)
    testImplementation(projects.content.interfaces.bank)
    testImplementation(projects.api.net)
    testImplementation(libs.rsprot.api)
    testImplementation(libs.fastutil)
    testImplementation("org.mockito:mockito-core:5.14.2")
    implementation(projects.api.attr)
    implementation(projects.api.repo)
    implementation(projects.content.quest)
    implementation(projects.content.skills.utils)
}

plugins {
    id("base-conventions")
    id("game-cache-test-conventions")
}

group = "org.rsmod.api"

kotlin { explicitApi() }

dependencies {
    testImplementation(libs.fastutil)
    implementation(libs.guice)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.engine.game)
}

plugins {
    id("base-conventions")

}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.config)
    implementation(projects.engine.game)
    implementation(projects.engine.module)
}

plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.quest)
}

testing.suites.named<JvmTestSuite>("integration") {
    dependencies {
        implementation(libs.guice)
        implementation(projects.api.combat.combatWeapon)
    }
}

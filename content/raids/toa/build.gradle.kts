plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    // AreaChecker, in PlayerTeleportValidateHook's signature (ToaTeleportHook).
    implementation(projects.api.areaChecker)
    // BossHpBarScript: the Palm of Resourcefulness progress bar (Crondis puzzle).
    implementation(projects.api.bossHpBarPlugin)
    // RegionRegistry.normalizeCoords (instance -> static coords, for challenge areas).
    implementation(projects.api.registry)
    // cureAllToxins on respawn, like the standard death.
    implementation(projects.api.mechanics.toxins)
}

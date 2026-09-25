plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.attr)
    // RegionRegistry.normalizeCoords (instance -> static coords, for challenge areas).
    implementation(projects.api.registry)
    // cureAllToxins on respawn, like the standard death.
    implementation(projects.api.mechanics.toxins)
}

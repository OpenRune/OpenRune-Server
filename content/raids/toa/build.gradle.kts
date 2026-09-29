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
    implementation(projects.api.bosses)
    // AccuracyFormulae: the Crondis crocodiles' accuracy roll.
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.combat.combatCommons)
    // RegionRegistry.normalizeCoords (instance -> static coords, for challenge areas).
    implementation(projects.api.registry)
    // cureAllToxins on respawn, like the standard death.
    implementation(projects.api.mechanics.toxins)
    // NetworkService / NpcInfo: extended NPC view inside the raid (ToaNpcView.kt). Same
    // dependency content/quest and content/other/login already use.
    implementation(libs.rsprot.api)
    implementation(projects.content.interfaces.bank)
    // ConsumableActivityGate / ToaPotionEffect: unlocking the Helpful Spirit's supplies in the
    // raid, and clearing their timed effects on death and on leaving.
    implementation(projects.content.other.consumables)
}

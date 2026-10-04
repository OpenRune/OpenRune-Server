package org.rsmod.content.bosses.araxxor

internal object AraxxorAssets {
    const val BOSS = "npc.araxxor"
    const val CORPSE = "npc.araxxor_dead"
    const val ACID = "loc.araxxor_acidpool"
    const val MELEE = "seq.npc_araxxor_01_attack_melee_01"
    const val RANGED = "seq.npc_araxxor_01_attack_ranged_01"
    const val MAGIC = "seq.npc_araxxor_01_attack_magic_01"
    const val ENRAGE = "seq.npc_araxxor_01_enrage_transition_01"
    const val CLEAVE = "seq.npc_araxxor_01_attack_melee_enraged_01"
    const val DEATH = "seq.npc_araxxor_01_death_01"
    const val HARVEST = "seq.npc_araxxor_01_death_loot_01"
    const val RANGED_PROJECTILE = "spotanim.araxxor_ranged_projectile"
    const val MAGIC_PROJECTILE = "spotanim.araxxor_magic_projectile"
    const val MAGIC_IMPACT = "spotanim.araxxor_magic_impact"
    const val ACID_PROJECTILE = "spotanim.araxxor_pools_proj"
    const val ACID_SPLASH = "spotanim.araxxor_pools_splash"
    val sequences = listOf(MELEE, RANGED, MAGIC, ENRAGE, CLEAVE, DEATH, HARVEST)
    val spots = listOf(RANGED_PROJECTILE, MAGIC_PROJECTILE, MAGIC_IMPACT, ACID_PROJECTILE, ACID_SPLASH)
}

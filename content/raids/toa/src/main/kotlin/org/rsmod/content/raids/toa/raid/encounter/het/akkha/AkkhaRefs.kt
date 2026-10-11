package org.rsmod.content.raids.toa.raid.encounter.het.akkha

internal object AkkhaNpcs {
    const val SPAWN = "npc.akkha_spawn"
    const val MELEE = "npc.akkha_melee"
    const val RANGE = "npc.akkha_range"
    const val MAGE = "npc.akkha_mage"
    const val ENRAGE_SPAWN = "npc.akkha_enrage_spawn"
    const val ENRAGE_INITIAL = "npc.akkha_enrage_initial"
    const val ENRAGE = "npc.akkha_enrage"
    const val SHADOW = "npc.akkha_shadow"
    const val SHADOW_ENRAGE = "npc.akkha_shadow_enrage"
    const val HEADBAR = "npc.akkha_headbar_npc"
    const val UNSTABLE_ORB = "npc.akkha_enrage_orb"
    const val ENRAGE_DUMMY = "npc.akkha_enrage_dummy"
    const val TRAIL_LIGHTNING = "npc.akkha_trail_orb_lightning"
    const val TRAIL_DARKNESS = "npc.akkha_trail_orb_darkness"
    const val TRAIL_BURN = "npc.akkha_trail_orb_burn"
    const val TRAIL_FREEZE = "npc.akkha_trail_orb_freeze"
}

internal object AkkhaObjs {
    const val ASHES = "obj.toa_akkha_ashes"
    const val BOOK = "obj.toa_book_akkha"
}

internal object AkkhaVarbits {
    const val BOOK_OWNED = "varbit.toa_book_akkha"
}

internal object AkkhaLocs {
    const val GLOW_DARKNESS = "loc.akkha_symbol_glow_darkness"
    const val GLOW_LIGHTNING = "loc.akkha_symbol_glow_lightning"
    const val GLOW_FREEZE = "loc.akkha_symbol_glow_freeze"
    const val GLOW_BURN = "loc.akkha_symbol_glow_burn"
}

internal object AkkhaSeqs {
    const val MELEE = "seq.npc_akkha_attack_melee_spear"
    const val RANGED = "seq.npc_akkha_attack_ranged_spear"
    const val MAGIC = "seq.npc_akkha_attack_magic_spear"
    const val DETONATE = "seq.npc_akkha_attack_magic_01_spear"
    const val MEMORY = "seq.npc_akkha_attack_magic_02_spear"
    const val TRAIL = "seq.npc_akkha_attack_magic_03_spear"
    const val FINAL_STAND = "seq.npc_akkha_attack_magic_04_spear"
    const val DISAPPEAR = "seq.npc_akkha_teleport_disappear_spear"
    const val APPEAR = "seq.npc_akkha_teleport_appear_spear"
    const val SHADOW_SPAWN = "seq.npc_akkha_spawn_spear"
    const val SHADOW_SLAM = "seq.npc_akkha_attack_magic_02_spear"
    const val DEATH = "seq.npc_akkha_death_spear"
}

internal object AkkhaSpots {
    const val RANGED_TRAVEL = "spotanim.akkha_ranged_travel"
    const val MAGIC_TRAVEL = "spotanim.akkha_magic_travel"
    const val MAGIC_IMPACT = "spotanim.akkha_magic_impact"
    const val DARKNESS = "spotanim.akkha_darkness"
    const val LIGHTNING = "spotanim.akkha_lightning"
    const val FREEZE = "spotanim.akkha_freeze"
    const val BURN = "spotanim.akkha_burn"
    const val ORB_IMPACT = "spotanim.akkha_enrage_orb_impact"
}

internal object AkkhaHeadbars {
    const val STOPWATCH = "headbar.charge_100"
    const val SHADOW_HEALTH = "headbar.health_70"
    const val AKKHA_HEALTH = "headbar.health_100"
}

internal object AkkhaMidis {
    val FIGHT: String? = null
}

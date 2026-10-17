package org.rsmod.content.raids.toa.raid.encounter.wardens

internal object WardenAbilities {
    const val AUTO = "auto"
    const val MELEE = "melee"
    const val RANGED = "ranged"
    const val MAGIC = "magic"
    const val SPECIAL = "special"
    const val DIVINE_MELEE = "divine_melee"
    const val DIVINE_RANGED = "divine_ranged"
    const val DIVINE_MAGIC = "divine_magic"
    const val ENTOMB = "entomb"
}

internal object WardenSeqs {
    const val MELEE = "seq.npc_wardens_melee01"
    const val RANGED = "seq.npc_wardens_ranged01"
    const val MAGIC = "seq.npc_wardens_magic01"
    const val ENTOMB = "seq.toa_wardens_entomb"
    const val ENTOMB_LOOP = "seq.toa_wardens_entomb_loop"
    const val ENTOMB_END = "seq.toa_wardens_entomb_end"
}

internal object WardenSpots {
    const val RANGED = "spotanim.projectile_warden_ranged01"
    const val MAGIC = "spotanim.fx_wardens_bomb01"
    const val PRAYER_MELEE_TRAVEL = "spotanim.toa_wardens_prayer_melee_travel"
    const val PRAYER_MELEE_IMPACT = "spotanim.toa_wardens_prayer_melee_impact"
    const val PRAYER_RANGED_TRAVEL = "spotanim.toa_wardens_prayer_ranged_travel"
    const val PRAYER_RANGED_IMPACT = "spotanim.toa_wardens_prayer_ranged_impact"
    const val PRAYER_MAGIC_TRAVEL = "spotanim.toa_wardens_prayer_magic_travel"
    const val PRAYER_MAGIC_IMPACT = "spotanim.toa_wardens_prayer_magic_impact"
    const val ENTOMB_TRAVEL = "spotanim.toa_wardens_entomb_travel"
    const val ENTOMB_MISS = "spotanim.toa_wardens_entomb_miss"
    const val ENTOMB_END = "spotanim.toa_wardens_entomb_end"
    const val SHADOW = "spotanim.gargboss_debris_shadow_120"
}

internal object WardenProjs {
    const val FLIGHT = "projanim.toa_wardens_flight"
    const val DIVINE = "projanim.toa_wardens_divine"
    const val ENTOMB = "projanim.toa_wardens_entomb"
}

internal object WardenMessages {
    const val SCIMITAR = "<col=ff3045>The warden throws an arcane scimitar.</col>"
    const val ARROW = "<col=229628>The warden fires an arcane arrow.</col>"
    const val SPELL = "<col=a53fff>The warden launches an arcane spell.</col>"
    const val PRAYERS_DISABLED = "<col=ff3045>Your protection prayers have been disabled!</col>"
    const val ENTOMBED = "<col=ff3045>You've been entombed in stone!</col>"
}

internal object WardenSounds {
    const val ENTOMB = "synth.toa_wardens_entomb"
    const val ENTOMB_RELEASE = "synth.fireblast_hit"
    const val ENTOMB_END = "synth.toa_wardens_entomb_end"
}

internal object WardenCoreRefs {
    const val KNEEL = "seq.npc_wardens_kneel01"
    const val STAND_UP = "seq.npc_wardens_standup01"
    const val BLOCKER = "loc.invisible_type8_blocking"
    const val SHIELD_BAR = "headbar.charge_140"
    const val HEALTH_BAR = "headbar.health_140"
    const val SPAWN_SOUND = "synth.toa_wardens_core_spawn"
    const val REVIVE_SOUND = "synth.toa_wardens_core_revive"
    const val PUSHED = "You are pushed off the core."
}

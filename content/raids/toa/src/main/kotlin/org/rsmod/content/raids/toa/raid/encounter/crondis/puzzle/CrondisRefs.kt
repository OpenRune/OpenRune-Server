package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

/** Gameval names used by the Crondis puzzle. Unnamed synths: gamevals.toml. */
internal object CrondisNpcs {
    /** The palm's growth stages. The last has no ops. */
    val PALMS =
        listOf(
            "npc.toa_crondis_tree_1",
            "npc.toa_crondis_tree_2",
            "npc.toa_crondis_tree_3",
            "npc.toa_crondis_tree_4",
            "npc.toa_crondis_tree_5",
        )
    val WATERABLE_PALMS = PALMS.dropLast(1)
    const val CROCODILE = "npc.toa_crondis_crocodile"
}

internal object CrondisObjs {
    const val CONTAINER = "obj.toa_crondis_water_container"
}

internal object CrondisLocs {
    const val WATER_SOURCE = "loc.toa_crondis_water_source"
    const val WATER_SOURCE_EMPTY = "loc.toa_crondis_water_source_empty"
    const val STATUE = "loc.toa_crondis_column_trap"
    const val ROW_TRAP = "loc.toa_crondis_row_trap"
    const val ROW_TRAP_FIRE = "loc.toa_crondis_row_trap_fire"
    const val ACID_ORB = "loc.toa_crondis_orb"
    const val PALM_BLOCKER = "loc.invisible_type8_blocking_size5"
    const val BARRIER = "loc.toa_path_barrier"
}

internal object CrondisSeqs {
    const val PICKUP = "seq.human_pickupfloor"
    const val TRAP_IDLE = "seq.crondis_spear_trap_idle"
    const val TRAP_ACTIVATE = "seq.crondis_spear_trap_activate"
    const val TRAP_SPEAR = "seq.crondis_spear_trap_spear02"
    const val CROC_ATTACK = "seq.croc_attack"
}

internal object CrondisSpots {
    const val ACID = "spotanim.crondis_column_trap_anim"
}

internal object CrondisSynths {
    const val TAKE = "synth.pick2"
    const val FILL = "synth.toa_crondis_fill_container"
    const val DECLINE = "synth.toa_crondis_water_empty"
    const val WATER_PALM = "synth.toa_crondis_water_tree_02"
    const val PALM_GROW = "synth.toa_crondis_tree_grow_02"
    const val PALM_SHRINK = "synth.toa_crondis_water_lost_02"
    const val SPILL = "synth.liquid"
}

internal object CrondisComponents {
    const val BAR_REMAINING = "component.hpbar_hud:health_bar_remaining"
}

internal object CrondisVarbits {
    const val PROTECT_FROM_MELEE = "varbit.prayer_protectfrommelee"
}

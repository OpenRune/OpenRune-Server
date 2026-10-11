package org.rsmod.content.raids.toa.raid.encounter.het.puzzle

internal object HetNpcs {
    const val SEAL = "npc.toa_het_goal"
    const val SEAL_WEAK = "npc.toa_het_goal_vulnerable"
    const val ORB = "npc.toa_het_orb"
}

internal object HetObjs {
    const val MIRROR = "obj.toa_het_mirror"
}

internal object HetLocs {
    const val MIRROR = "loc.toa_het_mirror"
    const val MIRROR_FIXED = "loc.toa_het_mirror_fixed"
    const val MIRROR_DIRTY = "loc.toa_het_mirror_dirty"
    const val GOAL_DEAD = "loc.toa_het_goal_dead"
    const val CASTER = "loc.toa_het_source"
    const val TARGET = "loc.toa_het_target"
    const val EXIT_OPEN = "loc.invisible_type8_nonblocking"
}

internal enum class HetBarrierType(
    val loc: String,
    val rising: String,
    val breakable: Boolean,
) {
    INTERNAL(
        "loc.toa_het_barrier_internal",
        "loc.toa_het_barrier_internal_rising",
        breakable = false,
    ),
    END(
        "loc.toa_het_barrier_end",
        "loc.toa_het_barrier_end_rising",
        breakable = false,
    ),
    BREAKABLE_INTERNAL(
        "loc.toa_het_barrier_destructable_internal",
        "loc.toa_het_barrier_destructable_internal_rising",
        breakable = true,
    ),
    BREAKABLE_END(
        "loc.toa_het_barrier_destructable_end",
        "loc.toa_het_barrier_destructable_end_rising",
        breakable = true,
    );

    companion object {
        val BREAKABLE_LOCS = entries.filter { it.breakable }.map { it.loc }
    }
}

internal object HetStatues {
    val LOCS =
        listOf(
            "loc.toa_het_statue_pickaxe_empty01",
            "loc.toa_het_statue_pickaxe_bronze01",
            "loc.toa_het_statue_pickaxe_iron01",
            "loc.toa_het_statue_pickaxe_steel01",
            "loc.toa_het_statue_pickaxe_black01",
            "loc.toa_het_statue_pickaxe_mithril01",
            "loc.toa_het_statue_pickaxe_adamant01",
            "loc.toa_het_statue_pickaxe_rune01",
            "loc.toa_het_statue_pickaxe_gilded01",
            "loc.toa_het_statue_pickaxe_dragon01",
            "loc.toa_het_statue_pickaxe_dragon02",
            "loc.toa_het_statue_pickaxe_dragon03",
            "loc.toa_het_statue_pickaxe_3rdage01",
            "loc.toa_het_statue_pickaxe_infernal01",
            "loc.toa_het_statue_pickaxe_crystal01",
            "loc.toa_het_statue_pickaxe_trailblazer01",
            "loc.toa_het_statue_pickaxe_leagues",
            "loc.toa_het_statue_pickaxe_trailblazer_reloaded",
        )
}

internal object HetSeqs {
    const val PICKUP = "seq.human_pickupfloor"
}

internal object HetSpots {
    const val BEAM_HORIZONTAL = "spotanim.fx_beam_attack03"
    const val BEAM_VERTICAL = "spotanim.fx_beam_attack01"
    const val BEAM_NORTH_EAST = "spotanim.fx_beam_attack08"
    const val BEAM_SOUTH_EAST = "spotanim.fx_beam_attack05"
    const val BEAM_SOUTH_WEST = "spotanim.fx_beam_attack06"
    const val BEAM_NORTH_WEST = "spotanim.fx_beam_attack07"
    const val BEAM_IMPACT = "spotanim.fx_beam_impact01"
    const val SEAL_GLOW = "spotanim.fx_obelisk01_glow01"
    const val SEAL_GLOW_WEAK = "spotanim.fx_obelisk01_glow02"
    const val ORB_POP = "spotanim.shadow_rush_impact"
    const val MIRROR_PLACE = "spotanim.shadow_burst_impact"
}

internal object HetSynths {
    const val PICK_UP = "synth.pick"
    const val PLACE = "synth.put_down"
    const val ORB_POP = "synth.shadow_rush_impact"
}

internal object HetMarks {
    const val SEAL_HEADBAR = 10
    const val SEAL_HITMARK = 43
    const val SEAL_HEADBAR_SEGMENTS = 100
}

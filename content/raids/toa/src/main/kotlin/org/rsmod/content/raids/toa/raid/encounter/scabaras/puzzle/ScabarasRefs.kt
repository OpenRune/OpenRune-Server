package org.rsmod.content.raids.toa.raid.encounter.scabaras.puzzle

internal object ScabarasNpcs {
    const val SCARAB = "npc.toa_scabaras_scarab"
    const val OBELISK = "npc.toa_scabaras_guesser_obelisk"
    const val OBELISK_LIT = "npc.toa_scabaras_guesser_obelisk_hit"
}

internal object ScabarasLocs {
    const val BARRIER = "loc.toa_scabaras_barrier"
    const val OPEN = "loc.invisible_type8_nonblocking"
    const val MEMORY_BUTTON = "loc.toa_scabaras_simonsays_button"
    const val MEMORY_UP = "loc.toa_scabaras_simonsays_tile_up"
    const val MEMORY_DOWN = "loc.toa_scabaras_simonsays_tile_down"
    const val MEMORY_PLAYER = "loc.toa_scabaras_simonsays_tile_down_player"
    const val SUM_TABLET = "loc.toa_scabaras_totaltiles_tablet"
    const val LIGHT_OFF = "loc.toa_scabaras_lightsout_tile_off"
    const val HOLE = "loc.toa_scabaras_spawn_hole"
    const val STEPPING_STONE = "loc.toa_scabaras_stepping_stone"

    fun sumTile(value: Int) = "loc.toa_scabaras_totaltiles_tile$value"

    fun matchHidden(symbol: Int) = "loc.toa_scabaras_memorygame_button$symbol"

    fun matchShown(symbol: Int) = "loc.toa_scabaras_memorygame_tile$symbol"

    fun glow(index: Int) = "loc.toa_scabaras_fx%02d".format(index)

    val MATCH_HIDDEN = (1..9).map(::matchHidden)
}

internal object ScabarasSeqs {
    const val PRESS_BUTTON = "seq.human_pickuptable"
    const val FLIP_START = "seq.toa_flip_tile"
    const val FLIP_END = "seq.human_pickupfloor"
    const val CRAWL = "seq.human_longcrawl"
    const val JUMP = "seq.human_spot_jump"
    const val SCARAB_SPAWN = "seq.npc_scarab_spawn"
    const val SCARAB_ATTACK = "seq.npc_scarab_ranged_attack"
}

internal object ScabarasSpots {
    const val ROCKFALL = "spotanim.ga_beast_rock_fall"
    const val SMALL_ROCKFALL = "spotanim.mm_roofcollapse"
    const val SCARAB_SHOT = "spotanim.toa_scarab_ranged_projectile01"
}

internal object ScabarasSynths {
    const val STEP = 6561
    const val WRONG = 6557
    const val COMPLETE = 2655
    const val BUTTON = 6559
    const val FLIP = 6560
    const val MISMATCH = 6554
    const val ROCKFALL = 6578
    const val CRAWL = 2454
    const val JUMP = 2461
}

internal object ScabarasMidis {
    const val PATH = "midi.toa_path_scabaras"
}

internal object ScabarasMessages {
    const val ALREADY_DONE = "You have already completed this puzzle."
    const val WAIT_SEQUENCE =
        "You must wait until the sequence has finished before you can request another."

    fun completed(label: Int) = "<col=229628>Puzzle $label has been completed!"

    fun sumGoal(goal: Int) =
        "The number @mes_hl_red@$goal</col> has been hastily chipped into the stone."
}

internal val SUM_GLOW = intArrayOf(0, 5, 6, 7, 8, 9, 10, 3, 11, 12)

internal val MATCH_GLOW = intArrayOf(0, 5, 6, 3, 8, 9, 4, 10, 11, 12)

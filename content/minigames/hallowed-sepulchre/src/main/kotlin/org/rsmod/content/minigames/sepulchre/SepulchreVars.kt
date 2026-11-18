package org.rsmod.content.minigames.sepulchre

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal var Player.timeRemaining by intVarBit("varbit.hallowed_time_remaining")
internal var Player.timeSpent by intVarBit("varbit.hallowed_time_spent")
internal var Player.floorTimeSpent by intVarBit("varbit.hallowed_current_floor_time_spent")
internal var Player.currentFloor by intVarBit("varbit.hallowed_current_floor")
internal var Player.timerPaused by boolVarBit("varbit.hallowed_block_climbover")
internal var Player.tutorialDone by boolVarBit("varbit.hallowed_tutorial")
internal var Player.skipEntryWarning by boolVarBit("varbit.hallowed_entry_check")
internal var Player.teleportsReceived by boolVarBit("varbit.hallowed_teleports_received")
internal var Player.checkpoint by intVarBit("varbit.hallowed_checkpoint")
internal var Player.startIndex by intVarBit("varbit.hallowed_start")
internal var Player.telepadImmunity by intVarBit("varbit.hallowed_telepad_immunity")
internal var Player.allFloorsBest by intVarBit("varbit.hallowed_time_pb_all")
internal var Player.coffinCount by intVarBit("varbit.hallowed_normal_chest_count")
internal var Player.grandCoffinCount by intVarBit("varbit.hallowed_final_chest_count")
internal var Player.busy by boolVarBit("varbit.busy")

internal const val OVERLAY = "interface.hallowed_overlay"
internal const val OVERLAY_TARGET = "component.toplevel_osrs_stretch:overlay_hud"
internal const val TIMER_UPDATE = "clientscript.[clientscript,hallowed_timer_update]"
internal const val TIMER_PAUSE = "clientscript.[clientscript,hallowed_timer_pause]"
internal const val FADE_LEGACY = "clientscript.[clientscript,fade_overlay_legacy]"
internal const val AGILITY = "stat.agility"

/**
 * Every treasure encounter's state varbit; arriving on a floor or leaving the Sepulchre clears
 * them, with the looted coffins and the skill xp the encounters have paid.
 */
internal val FLOOR_STATE_VARBITS =
    listOf(
        "varbit.hallowed_ranged_state",
        "varbit.hallowed_prayer_state",
        "varbit.hallowed_construction_state",
        "varbit.hallowed_magic_state",
        "varbit.hallowed_prayer_ashes_offered",
        "varbit.hallowed_magic_spell_failed",
        "varbit.hallowed_current_floor_looted",
        "varbit.hallowed_current_floor_looted_b",
        "varbit.hallowed_grapple_type",
        "varbit.hallowed_final_floor_looted",
        "varbit.hallowed_floor5_grapple_looted",
        "varbit.hallowed_floor5_portal_looted",
        "varbit.hallowed_floor5_bridge_looted",
        "varbit.hallowed_ranged_xp_earned",
        "varbit.hallowed_magic_xp_earned",
        "varbit.hallowed_prayer_xp_earned",
        "varbit.hallowed_construction_xp_earned",
    )

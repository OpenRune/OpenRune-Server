package org.rsmod.content.quest.mudfoot

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal object MudfootStage {
    const val NOT_STARTED = 0
    const val STARTED = 2
    const val FIND_GOBLINS = 4
    const val SIDEKICK = 6
    const val MET_HARLAN = 8
    const val WINDMILL = 10
    const val RANGER_FIGHT = 12
    const val RANGER_DEAD = 14
    const val MAGE_BRIEFED = 16
    const val MAGE_FIGHT = 18
    const val MAGE_DEAD = 20
    const val WARRIOR_BRIEFED = 22
    const val WARRIOR_FIGHT = 24
    const val WARRIOR_DEAD = 26
    const val RETURN_TO_HARLAN = 28
    const val MUDFOOT_LEFT = 30
    const val COMPLETE = 32
}

/** Values of `varbit.mba_mudfoot_vis`: which Mudfoot the client shows. */
internal object MudfootVis {
    const val HIDDEN = 0
    const val GOBLIN_HOUSE = 1
    const val FOLLOWING = 2
    const val WINDMILL = 3
    const val WOODS = 4
    const val GRAVEYARD = 5
}

internal var Player.harlanIntroduced by boolVarBit("varbit.captain_harlan_intro")
internal var Player.mudfootVis by intVarBit("varbit.mba_mudfoot_vis")
internal var Player.harlanMetMudfootEarly by boolVarBit("varbit.captain_harlan_met_mudfoot_early")
internal var Player.harlanMeleeGiveaway by boolVarBit("varbit.captain_harlan_bonus_melee_giveaway")
internal var Player.harlanRangedGiveaway by boolVarBit("varbit.captain_harlan_bonus_ranged_giveaway")
internal var Player.harlanMagicGiveaway by boolVarBit("varbit.captain_harlan_bonus_magic_giveaway")
internal var Player.mudfootFirstThiefDialogue by boolVarBit("varbit.mudfoot_first_thief_dialogue")
internal var Player.mudfootLampsUsed by intVarBit("varbit.mba_lamps")

/** Packed coords to put the player back on if they log out inside an encounter. */
internal var Player.mudfootReturnTile by intVarp("varp.mba_return_tile")

/** Epoch minute of Captain Harlan's last weapon handout, for the 30 minute cooldown. */
internal var Player.harlanWeaponClaimMinute by intVarp("varp.mba_weapon_claim_minute")

internal fun restoreMudfootLogin(player: Player) {
    val packed = player.mudfootReturnTile
    if (packed == 0) {
        return
    }
    player.coords = CoordGrid(packed)
    player.mudfootReturnTile = 0
}
internal var Player.mudfootStage by intVarBit("varbit.mba")

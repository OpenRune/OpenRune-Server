package org.rsmod.content.areas.misc.stronghold_of_security

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal enum class StrongholdFloor(
    val doorTitle: String,
    val doorNpc: String,
    val face: String,
    val mirror: String,
    val portal: String,
    val reward: String,
    val emoteName: String,
    val start: CoordGrid,
    val rewardRoom: CoordGrid,
    val portalCombatLevel: Int?,
    val coins: Int,
) {
    War(
        doorTitle = "Gate of War",
        doorNpc = "npc.sos_door_war",
        face = "loc.sos_war_door_face",
        mirror = "loc.sos_war_door_face_mirr",
        portal = "loc.sos_war_portal",
        reward = "loc.sos_war_chest",
        emoteName = "Flap",
        start = CoordGrid(1861, 5241, 0),
        rewardRoom = CoordGrid(1906, 5222, 0),
        portalCombatLevel = 26,
        coins = 2_000,
    ),
    Famine(
        doorTitle = "Rickety door",
        doorNpc = "npc.sos_door_fam",
        face = "loc.sos_fam_door_face",
        mirror = "loc.sos_fam_door_face_mirr",
        portal = "loc.sos_fam_portal",
        reward = "loc.sos_fam_sack",
        emoteName = "Slap Head",
        start = CoordGrid(2042, 5243, 0),
        rewardRoom = CoordGrid(2021, 5214, 0),
        portalCombatLevel = 51,
        coins = 3_000,
    ),
    Pestilence(
        doorTitle = "Oozing barrier",
        doorNpc = "npc.sos_door_pest",
        face = "loc.sos_pest_door_face",
        mirror = "loc.sos_pest_door_face_mirr",
        portal = "loc.sos_pest_portal",
        reward = "loc.sos_pest_chest",
        emoteName = "Idea",
        start = CoordGrid(2123, 5254, 0),
        rewardRoom = CoordGrid(2144, 5279, 0),
        portalCombatLevel = 76,
        coins = 5_000,
    ),
    Death(
        doorTitle = "Portal of Death",
        doorNpc = "npc.sos_door_death",
        face = "loc.sos_death_door_face",
        mirror = "loc.sos_death_door_face_mirr",
        portal = "loc.sos_death_portal",
        reward = "loc.sos_death_pram",
        emoteName = "Stamp",
        start = CoordGrid(2362, 5214, 0),
        rewardRoom = CoordGrid(2344, 5213, 0),
        portalCombatLevel = null,
        coins = 0,
    ),
}

private var Player.warClaimed by boolVarBit("varbit.sos_war_claimed")
private var Player.famineClaimed by boolVarBit("varbit.sos_fam_claimed")
private var Player.pestilenceClaimed by boolVarBit("varbit.sos_pest_claimed")
private var Player.deathClaimed by boolVarBit("varbit.sos_death_claimed")

private var Player.flapUnlocked by boolVarBit("varbit.sos_emote_flap")
private var Player.slapHeadUnlocked by boolVarBit("varbit.sos_emote_doh")
private var Player.ideaUnlocked by boolVarBit("varbit.sos_emote_idea")
private var Player.stampUnlocked by boolVarBit("varbit.sos_emote_stamp")

internal fun Player.hasClaimed(floor: StrongholdFloor): Boolean =
    when (floor) {
        StrongholdFloor.War -> warClaimed
        StrongholdFloor.Famine -> famineClaimed
        StrongholdFloor.Pestilence -> pestilenceClaimed
        StrongholdFloor.Death -> deathClaimed
    }

internal fun Player.markClaimed(floor: StrongholdFloor) {
    when (floor) {
        StrongholdFloor.War -> warClaimed = true
        StrongholdFloor.Famine -> famineClaimed = true
        StrongholdFloor.Pestilence -> pestilenceClaimed = true
        StrongholdFloor.Death -> deathClaimed = true
    }
}

internal fun Player.unlockEmote(floor: StrongholdFloor) {
    when (floor) {
        StrongholdFloor.War -> flapUnlocked = true
        StrongholdFloor.Famine -> slapHeadUnlocked = true
        StrongholdFloor.Pestilence -> ideaUnlocked = true
        StrongholdFloor.Death -> stampUnlocked = true
    }
}

internal fun Player.completedStronghold(): Boolean = StrongholdFloor.entries.all { hasClaimed(it) }

internal fun Player.doorsStayQuiet(floor: StrongholdFloor): Boolean =
    hasClaimed(floor) || completedStronghold()

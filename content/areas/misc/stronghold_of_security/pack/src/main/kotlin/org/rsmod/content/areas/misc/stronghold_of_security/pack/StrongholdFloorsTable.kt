package org.rsmod.content.areas.misc.stronghold_of_security.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object StrongholdFloorsTable {

    const val DOOR_TITLE = 0
    const val DOOR_NPC = 1
    const val DOOR_FACE = 2
    const val DOOR_MIRROR = 3
    const val PORTAL = 4
    const val PORTAL_COMBAT_LEVEL = 5
    const val START = 6
    const val REWARD_ROOM = 7
    const val REWARD = 8
    const val REWARD_COINS = 9
    const val REWARD_TEXT = 10
    const val RESTORES_ALL_STATS = 11
    const val CLAIMED_VARBIT = 12
    const val EMOTE = 13
    const val EMOTE_VARBIT = 14

    fun strongholdFloors() = dbTable("dbtable.stronghold_floors", serverOnly = true) {
        column("door_title", DOOR_TITLE, VarType.STRING)
        column("door_npc", DOOR_NPC, VarType.NPC)
        column("door_face", DOOR_FACE, VarType.LOC)
        column("door_mirror", DOOR_MIRROR, VarType.LOC)
        column("portal", PORTAL, VarType.LOC)
        column("portal_combat_level", PORTAL_COMBAT_LEVEL, VarType.INT)
        column("start", START, VarType.COORDGRID)
        column("reward_room", REWARD_ROOM, VarType.COORDGRID)
        column("reward", REWARD, VarType.LOC)
        column("reward_coins", REWARD_COINS, VarType.INT)
        column("reward_text", REWARD_TEXT, VarType.STRING)
        column("restores_all_stats", RESTORES_ALL_STATS, VarType.BOOLEAN)
        column("claimed_varbit", CLAIMED_VARBIT, VarType.INT)
        column("emote", EMOTE, VarType.STRING)
        column("emote_varbit", EMOTE_VARBIT, VarType.INT)

        row("dbrow.stronghold_floor_war") {
            column(DOOR_TITLE, "Gate of War")
            columnRSCM(DOOR_NPC, "npc.sos_door_war")
            columnRSCM(DOOR_FACE, "loc.sos_war_door_face")
            columnRSCM(DOOR_MIRROR, "loc.sos_war_door_face_mirr")
            columnRSCM(PORTAL, "loc.sos_war_portal")
            column(PORTAL_COMBAT_LEVEL, 26)
            columnCoord(START, CoordGrid(1861, 5241, 0))
            columnCoord(REWARD_ROOM, CoordGrid(1906, 5222, 0))
            columnRSCM(REWARD, "loc.sos_war_chest")
            column(REWARD_COINS, 2_000)
            column(
                REWARD_TEXT,
                "You open the Gift of Peace and find 2,000 coins. A great calm washes over you, " +
                    "restoring your hitpoints and prayer points.",
            )
            column(RESTORES_ALL_STATS, false)
            columnRSCM(CLAIMED_VARBIT, "varbit.sos_war_claimed")
            column(EMOTE, "Flap")
            columnRSCM(EMOTE_VARBIT, "varbit.sos_emote_flap")
        }

        row("dbrow.stronghold_floor_famine") {
            column(DOOR_TITLE, "Rickety door")
            columnRSCM(DOOR_NPC, "npc.sos_door_fam")
            columnRSCM(DOOR_FACE, "loc.sos_fam_door_face")
            columnRSCM(DOOR_MIRROR, "loc.sos_fam_door_face_mirr")
            columnRSCM(PORTAL, "loc.sos_fam_portal")
            column(PORTAL_COMBAT_LEVEL, 51)
            columnCoord(START, CoordGrid(2042, 5243, 0))
            columnCoord(REWARD_ROOM, CoordGrid(2021, 5214, 0))
            columnRSCM(REWARD, "loc.sos_fam_sack")
            column(REWARD_COINS, 3_000)
            column(
                REWARD_TEXT,
                "You search the Grain of Plenty and find 3,000 coins. You feel nourished, and " +
                    "your hitpoints and prayer points are restored.",
            )
            column(RESTORES_ALL_STATS, false)
            columnRSCM(CLAIMED_VARBIT, "varbit.sos_fam_claimed")
            column(EMOTE, "Slap Head")
            columnRSCM(EMOTE_VARBIT, "varbit.sos_emote_doh")
        }

        row("dbrow.stronghold_floor_pestilence") {
            column(DOOR_TITLE, "Oozing barrier")
            columnRSCM(DOOR_NPC, "npc.sos_door_pest")
            columnRSCM(DOOR_FACE, "loc.sos_pest_door_face")
            columnRSCM(DOOR_MIRROR, "loc.sos_pest_door_face_mirr")
            columnRSCM(PORTAL, "loc.sos_pest_portal")
            column(PORTAL_COMBAT_LEVEL, 76)
            columnCoord(START, CoordGrid(2123, 5254, 0))
            columnCoord(REWARD_ROOM, CoordGrid(2144, 5279, 0))
            columnRSCM(REWARD, "loc.sos_pest_chest")
            column(REWARD_COINS, 5_000)
            column(
                REWARD_TEXT,
                "You open the Box of Health and find 5,000 coins. The medicine inside restores " +
                    "all of your stats.",
            )
            column(RESTORES_ALL_STATS, true)
            columnRSCM(CLAIMED_VARBIT, "varbit.sos_pest_claimed")
            column(EMOTE, "Idea")
            columnRSCM(EMOTE_VARBIT, "varbit.sos_emote_idea")
        }

        row("dbrow.stronghold_floor_death") {
            column(DOOR_TITLE, "Portal of Death")
            columnRSCM(DOOR_NPC, "npc.sos_door_death")
            columnRSCM(DOOR_FACE, "loc.sos_death_door_face")
            columnRSCM(DOOR_MIRROR, "loc.sos_death_door_face_mirr")
            columnRSCM(PORTAL, "loc.sos_death_portal")
            columnCoord(START, CoordGrid(2362, 5214, 0))
            columnCoord(REWARD_ROOM, CoordGrid(2344, 5213, 0))
            columnRSCM(REWARD, "loc.sos_death_pram")
            column(RESTORES_ALL_STATS, false)
            columnRSCM(CLAIMED_VARBIT, "varbit.sos_death_claimed")
            column(EMOTE, "Stamp")
            columnRSCM(EMOTE_VARBIT, "varbit.sos_emote_stamp")
        }
    }
}

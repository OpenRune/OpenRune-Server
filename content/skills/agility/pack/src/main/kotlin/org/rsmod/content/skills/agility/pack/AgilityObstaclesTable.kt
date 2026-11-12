package org.rsmod.content.skills.agility.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/**
 * Every course obstacle in lap order. A lane is one way across: the player is put on its start
 * tile and its actions play out tick by tick, `land` granting the obstacle's xp. An obstacle with a
 * success curve can fail, which plays the matching fail lane instead, with its own messages and
 * says; a lane without a fail lane never fails. Xp is in tenths.
 *
 * Several obstacles of one course can share a loc (the Barbarian Outpost walls), as can courses
 * that start alike and split later (Shayzien, Colossal Wyrm); each is its own row. A row with step
 * [RECOVERY_STEP] is a way back onto a course after a fall and is not part of the lap. A row with
 * npcs instead of locs and lanes is a hand-in to that npc, as the Werewolf course's stick. The
 * Brimhaven Agility Arena has no laps, so its obstacles take [RECOVERY_STEP] too. A row with lanes
 * but neither locs nor npcs is stepped on: it runs the tick after a player stops on one of its
 * lanes' start tiles, as the Agility Pyramid's stone blocks.
 * An obstacle can also need levels in other stats and pay xp in them (`stats`), set a varbit as
 * it starts (`start_varbit`) and break carried objs (`breaks`), as Dorgesh-Kaan's parts.
 * A row without a course is an agility shortcut: it needs its own `level`, `quest` and varbit
 * ranges (`varbit_gates`, refused with `refusal`), answers its locs' `op` and is part of no lap.
 * Below its level it shows `level_refusal`, beside `level_refusal_obj` when set.
 * A `walk_on` row also runs as a player walks onto a lane's start, as the Penguin course's sled
 * jumps, whether or not it has locs.
 */
object AgilityObstaclesTable {
    const val COURSE = 0
    const val STEP = 1
    const val LOCS = 2
    const val XP = 3
    const val LANES = 4
    const val ACTIONS = 5
    const val MESSAGES = 6
    const val SUCCESS = 7
    const val FAIL_ACTIONS = 8
    const val FAIL_DAMAGE = 9
    const val DIARY_XP = 10
    const val AP_RANGE = 11
    const val REQUIREMENT = 12
    const val LAND_SPAWN_OBJ = 13
    const val LAND_SPAWN_TILES = 14
    const val REQUIREMENT_OBJS = 15
    const val SAYS = 16
    const val NPCS = 17
    const val HAND_IN_OBJ = 18
    const val HAND_IN_MESSAGES = 19
    const val LAND_MESBOX = 20
    const val STATS = 21
    const val START_VARBIT = 22
    const val BREAKS = 23
    const val LEVEL = 24
    const val QUEST = 25
    const val VARBIT_GATES = 26
    const val OP = 27
    const val REFUSAL = 28
    const val FAIL_MESSAGES = 29
    const val FAIL_SAYS = 30
    const val WALK_ON = 31
    const val LEVEL_REFUSAL = 32
    const val LEVEL_REFUSAL_OBJ = 33

    const val ACTION_STEP = 0
    const val ACTION_FACE = 1
    const val ACTION_ANIM = 2
    const val ACTION_RESET_ANIM = 3
    const val ACTION_SOUND = 4
    const val ACTION_BALANCE = 5
    const val ACTION_BALANCE_OFF = 6
    const val ACTION_EXACT_MOVE = 7
    const val ACTION_TELEJUMP = 8
    const val ACTION_LAND = 9
    const val ACTION_HIT = 10
    const val ACTION_EXACT_PATH = 11
    const val ACTION_FACE_ANGLE = 12
    const val ACTION_WAIT = 13
    const val ACTION_XP = 14
    const val ACTION_VARBIT = 15
    const val ACTION_SPOTANIM = 18
    const val ACTION_LOC_ANIM = 19
    const val ACTION_NPC_SAY = 22
    const val ACTION_MESBOX = 26

    fun obstacles() =
        dbTable("dbtable.agility_obstacles", serverOnly = true) {
            column("course", COURSE, VarType.DBROW)
            column("step", STEP, VarType.INT)
            column("locs", LOCS, VarType.LOC)
            column("xp", XP, VarType.INT)
            column("lanes", LANES, VarType.INT, VarType.COORDGRID)
            column("actions", ACTIONS, *ACTION_TYPES)
            column("messages", MESSAGES, VarType.INT, VarType.INT, VarType.STRING)
            column("success", SUCCESS, VarType.INT, VarType.INT)
            column("fail_actions", FAIL_ACTIONS, *ACTION_TYPES)
            column("fail_damage", FAIL_DAMAGE, VarType.INT, VarType.INT, VarType.INT, VarType.INT)
            column("diary_xp", DIARY_XP, VarType.INT, VarType.INT)
            column("ap_range", AP_RANGE, VarType.INT)
            column("requirement", REQUIREMENT, VarType.INT)
            column("land_spawn_obj", LAND_SPAWN_OBJ, VarType.OBJ)
            column("land_spawn_tiles", LAND_SPAWN_TILES, VarType.COORDGRID)
            column("requirement_objs", REQUIREMENT_OBJS, VarType.OBJ)
            column("says", SAYS, VarType.INT, VarType.INT, VarType.STRING)
            column("npcs", NPCS, VarType.NPC)
            column("hand_in_obj", HAND_IN_OBJ, VarType.OBJ)
            column("hand_in_messages", HAND_IN_MESSAGES, VarType.STRING, VarType.STRING)
            column("land_mesbox", LAND_MESBOX, VarType.STRING)
            column("stats", STATS, VarType.STAT, VarType.INT, VarType.INT)
            column("start_varbit", START_VARBIT, VarType.INT, VarType.INT)
            column("breaks", BREAKS, VarType.ENUM)
            column("level", LEVEL, VarType.INT)
            column("quest", QUEST, VarType.DBROW)
            column("varbit_gates", VARBIT_GATES, VarType.INT, VarType.INT, VarType.INT)
            column("op", OP, VarType.INT)
            column("refusal", REFUSAL, VarType.STRING)
            column("fail_messages", FAIL_MESSAGES, VarType.INT, VarType.INT, VarType.STRING)
            column("fail_says", FAIL_SAYS, VarType.INT, VarType.INT, VarType.STRING)
            column("walk_on", WALK_ON, VarType.BOOLEAN)
            column("level_refusal", LEVEL_REFUSAL, VarType.STRING)
            column("level_refusal_obj", LEVEL_REFUSAL_OBJ, VarType.OBJ)

        }

    const val RECOVERY_STEP = -1

    const val REQUIREMENT_NINJA_GREEGREE = 1
    const val REQUIREMENT_CROSSBOW_GRAPPLE = 2
    const val REQUIREMENT_BARE_HEAD = 3
    const val REQUIREMENT_WORN = 4
    const val REQUIREMENT_NOT_CARRIED = 5

    private val ACTION_TYPES =
        arrayOf(
            VarType.INT,
            VarType.INT,
            VarType.INT,
            VarType.COORDGRID,
            VarType.INT,
            VarType.INT,
            VarType.INT,
        )
}

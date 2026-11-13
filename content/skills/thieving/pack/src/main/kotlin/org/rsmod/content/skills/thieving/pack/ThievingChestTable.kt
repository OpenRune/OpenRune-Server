package org.rsmod.content.skills.thieving.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object ThievingChestTable {
    const val COL_LOC = 0
    const val COL_OPEN_LOC = 1
    const val COL_EMPTY_LOC = 2
    const val COL_LEVEL = 3
    const val COL_XP = 4
    const val COL_RESPAWN_TICKS = 5
    const val COL_METHOD = 6
    const val COL_NEEDS_LOCKPICK = 7
    const val COL_TRAP_DAMAGE_SCALE = 8
    const val COL_TRAP_DAMAGE_BASE = 9
    const val COL_SUCCESS_LOW = 10
    const val COL_SUCCESS_HIGH = 11
    const val COL_LOCKPICK_BONUS = 12
    const val COL_TELEPORT_LOW = 13
    const val COL_TELEPORT_HIGH = 14
    const val COL_TELEPORT = 15
    const val COL_TELEPORT_RADIUS = 16
    const val COL_LOOT_DELAY = 17
    const val COL_GUARD = 18
    const val COL_XERIC_TELEPORT = 19
    const val COL_ATTEMPT_SOUND = 20
    const val COL_DESCRIBE_LOOT = 21

    const val METHOD_TRAPPED = 0
    const val METHOD_LOCKED = 1
    const val METHOD_PICKLOCK = 2

    private const val PICKLOCK_RESPAWN_TICKS = 2
    private const val PICKLOCK_LOCKPICK_BONUS = 25
    private const val TELEPORT_AREA_RADIUS = 3

    fun chests() =
        dbTable("dbtable.thieving_chest_config", serverOnly = true) {
            column("loc", COL_LOC, VarType.LOC)
            column("open_loc", COL_OPEN_LOC, VarType.LOC)
            column("empty_loc", COL_EMPTY_LOC, VarType.LOC)
            column("level", COL_LEVEL, VarType.INT)
            column("xp", COL_XP, VarType.INT)
            column("respawn_ticks", COL_RESPAWN_TICKS, VarType.INT)
            column("method", COL_METHOD, VarType.INT)
            column("needs_lockpick", COL_NEEDS_LOCKPICK, VarType.BOOLEAN)
            column("trap_damage_scale", COL_TRAP_DAMAGE_SCALE, VarType.INT)
            column("trap_damage_base", COL_TRAP_DAMAGE_BASE, VarType.INT)
            column("success_low", COL_SUCCESS_LOW, VarType.INT)
            column("success_high", COL_SUCCESS_HIGH, VarType.INT)
            column("lockpick_bonus", COL_LOCKPICK_BONUS, VarType.INT)
            column("teleport_low", COL_TELEPORT_LOW, VarType.INT)
            column("teleport_high", COL_TELEPORT_HIGH, VarType.INT)
            column("teleport", COL_TELEPORT, VarType.COORDGRID)
            column("teleport_radius", COL_TELEPORT_RADIUS, VarType.INT)
            column("loot_delay", COL_LOOT_DELAY, VarType.INT)
            column("guard", COL_GUARD, VarType.NPC)
            column("xeric_teleport", COL_XERIC_TELEPORT, VarType.BOOLEAN)
            column("attempt_sound", COL_ATTEMPT_SOUND, VarType.BOOLEAN)
            column("describe_loot", COL_DESCRIBE_LOOT, VarType.BOOLEAN)

            fun chest(
                row: String,
                loc: String,
                openLoc: String,
                level: Int,
                xp: Int,
                respawnTicks: Int,
                method: Int,
                emptyLoc: String? = null,
                needsLockpick: Boolean = false,
                trapDamageScale: Int = 0,
                trapDamageBase: Int = 0,
                successLow: Int = 0,
                successHigh: Int = 0,
                lockpickBonus: Int = 0,
                teleportLow: Int = 0,
                teleportHigh: Int = 0,
                teleport: CoordGrid? = null,
                teleportRadius: Int = 0,
                lootDelay: Int = 0,
                guard: String? = null,
                xericTeleport: Boolean = false,
                attemptSound: Boolean = false,
                describeLoot: Boolean = false,
            ) =
                row(row) {
                    columnRSCM(COL_LOC, loc)
                    columnRSCM(COL_OPEN_LOC, openLoc)
                    if (emptyLoc != null) columnRSCM(COL_EMPTY_LOC, emptyLoc)
                    column(COL_LEVEL, level)
                    column(COL_XP, xp)
                    column(COL_RESPAWN_TICKS, respawnTicks)
                    column(COL_METHOD, method)
                    column(COL_NEEDS_LOCKPICK, needsLockpick)
                    column(COL_TRAP_DAMAGE_SCALE, trapDamageScale)
                    column(COL_TRAP_DAMAGE_BASE, trapDamageBase)
                    column(COL_SUCCESS_LOW, successLow)
                    column(COL_SUCCESS_HIGH, successHigh)
                    column(COL_LOCKPICK_BONUS, lockpickBonus)
                    column(COL_TELEPORT_LOW, teleportLow)
                    column(COL_TELEPORT_HIGH, teleportHigh)
                    if (teleport != null) columnCoord(COL_TELEPORT, teleport)
                    column(COL_TELEPORT_RADIUS, teleportRadius)
                    column(COL_LOOT_DELAY, lootDelay)
                    if (guard != null) columnRSCM(COL_GUARD, guard)
                    column(COL_XERIC_TELEPORT, xericTeleport)
                    column(COL_ATTEMPT_SOUND, attemptSound)
                    column(COL_DESCRIBE_LOOT, describeLoot)
                }

            /*
             * Trap damage is (hitpoints * scale + base) / 100. Nature rune, blood rune and Rogues'
             * Castle chests are known. UNVERIFIED: the 10 coin, 50 coin, steel arrowtips and
             * Ardougne Castle chests reuse the nearest known formula.
             */
            fun trapped(
                row: String,
                loc: String,
                level: Int,
                xp: Int,
                respawnTicks: Int,
                trapDamageScale: Int,
                trapDamageBase: Int,
                emptyLoc: String = "loc.emptytrapchest",
                needsLockpick: Boolean = false,
                teleport: CoordGrid? = null,
            ) =
                chest(
                    row,
                    loc = loc,
                    openLoc = "loc.inacopenchest",
                    emptyLoc = emptyLoc,
                    level = level,
                    xp = xp,
                    respawnTicks = respawnTicks,
                    method = METHOD_TRAPPED,
                    needsLockpick = needsLockpick,
                    trapDamageScale = trapDamageScale,
                    trapDamageBase = trapDamageBase,
                    teleport = teleport,
                )

            fun pirate(
                row: String,
                loc: String,
                openLoc: String,
                level: Int,
                xp: Int,
                teleport: CoordGrid?,
            ) =
                chest(
                    row,
                    loc = loc,
                    openLoc = openLoc,
                    level = level,
                    xp = xp,
                    respawnTicks = PICKLOCK_RESPAWN_TICKS,
                    method = METHOD_PICKLOCK,
                    successLow = -5,
                    successHigh = 155,
                    lockpickBonus = PICKLOCK_LOCKPICK_BONUS,
                    teleportLow = 50,
                    teleportHigh = 20,
                    teleport = teleport,
                    teleportRadius = TELEPORT_AREA_RADIUS,
                    lootDelay = 2,
                )

            trapped(
                "dbrow.thieving_chest_10_coins",
                loc = "loc.trapchest1",
                level = 13,
                xp = 78,
                respawnTicks = 6,
                trapDamageScale = 12,
                trapDamageBase = 300,
            )
            trapped(
                "dbrow.thieving_chest_nature_rune",
                loc = "loc.trapchest2",
                level = 28,
                xp = 250,
                respawnTicks = 13,
                trapDamageScale = 12,
                trapDamageBase = 300,
            )
            trapped(
                "dbrow.thieving_chest_50_coins",
                loc = "loc.trapchest3",
                level = 43,
                xp = 1250,
                respawnTicks = 75,
                trapDamageScale = 15,
                trapDamageBase = 300,
            )
            trapped(
                "dbrow.thieving_chest_steel_arrowtips",
                loc = "loc.pickchest3",
                level = 47,
                xp = 1500,
                respawnTicks = 125,
                trapDamageScale = 15,
                trapDamageBase = 300,
                emptyLoc = "loc.emptypickchest",
                needsLockpick = true,
            )
            trapped(
                "dbrow.thieving_chest_blood_rune",
                loc = "loc.trapchest4",
                level = 59,
                xp = 2500,
                respawnTicks = 200,
                trapDamageScale = 18,
                trapDamageBase = 450,
                teleport = CoordGrid(2584, 3337),
            )
            // UNVERIFIED: the Witchaven tile the Ardougne Castle chest's second trap sends you to.
            trapped(
                "dbrow.thieving_chest_ardougne_castle",
                loc = "loc.trapchest5",
                level = 72,
                xp = 5000,
                respawnTicks = 400,
                trapDamageScale = 18,
                trapDamageBase = 450,
                teleport = CoordGrid(2727, 3283),
            )
            chest(
                "dbrow.thieving_chest_rogues_castle",
                loc = "loc.wilderness_rogue_chest",
                openLoc = "loc.wilderness_rogue_chest_open",
                level = 84,
                xp = 7017,
                respawnTicks = 34,
                method = METHOD_TRAPPED,
                trapDamageScale = 15,
                trapDamageBase = 300,
                guard = "npc.wilderness_rogue",
                describeLoot = true,
            )
            chest(
                "dbrow.thieving_chest_dorgesh_average",
                loc = "loc.dorgesh_average_chest_closed",
                openLoc = "loc.dorgesh_average_chest_open",
                emptyLoc = "loc.dorgesh_average_chest_closed_empty",
                level = 52,
                xp = 2000,
                respawnTicks = 150,
                method = METHOD_LOCKED,
                needsLockpick = true,
            )
            chest(
                "dbrow.thieving_chest_dorgesh_rich",
                loc = "loc.dorgesh_rich_chest_closed",
                openLoc = "loc.dorgesh_rich_chest_open",
                emptyLoc = "loc.dorgesh_rich_chest_closed_empty",
                level = 78,
                xp = 6500,
                respawnTicks = 500,
                method = METHOD_LOCKED,
                needsLockpick = true,
            )
            chest(
                "dbrow.thieving_chest_isle_of_souls",
                loc = "loc.sw_dungeon_thieving_chest_closed",
                openLoc = "loc.sw_dungeon_thieving_chest_open",
                level = 28,
                xp = 1500,
                respawnTicks = PICKLOCK_RESPAWN_TICKS,
                method = METHOD_PICKLOCK,
                successLow = -23,
                successHigh = 153,
                lockpickBonus = 26,
                teleportLow = 42,
                teleportHigh = 42,
                teleport = CoordGrid(2325, 2933),
                teleportRadius = TELEPORT_AREA_RADIUS,
                lootDelay = 1,
                attemptSound = true,
            )
            chest(
                "dbrow.thieving_chest_aldarin_villas",
                loc = "loc.aldarin_thieving_chest",
                openLoc = "loc.aldarin_thieving_chest_open",
                level = 36,
                xp = 2000,
                respawnTicks = 3,
                method = METHOD_PICKLOCK,
                successLow = 0,
                successHigh = 150,
                lockpickBonus = PICKLOCK_LOCKPICK_BONUS,
                teleportLow = 73,
                teleportHigh = 17,
                teleport = CoordGrid(1379, 2971),
                teleportRadius = TELEPORT_AREA_RADIUS,
                lootDelay = 2,
            )
            chest(
                "dbrow.thieving_chest_stone",
                loc = "loc.molch_temple_chest_theiving",
                openLoc = "loc.molch_temple_chest_theiving_open",
                level = 64,
                xp = 2800,
                respawnTicks = PICKLOCK_RESPAWN_TICKS,
                method = METHOD_PICKLOCK,
                successLow = -56,
                successHigh = 154,
                lockpickBonus = PICKLOCK_LOCKPICK_BONUS,
                teleportLow = 31,
                teleportHigh = 31,
                teleport = CoordGrid(1314, 3679),
                lootDelay = 2,
                xericTeleport = true,
            )
            // UNVERIFIED: where the rusty chests' trap sends you, so they never teleport.
            pirate(
                "dbrow.thieving_chest_rusty_dognose",
                loc = "loc.rusty_pirate_thieving_chest_dognose",
                openLoc = "loc.rusty_pirate_thieving_chest_open",
                level = 33,
                xp = 900,
                teleport = null,
            )
            pirate(
                "dbrow.thieving_chest_rusty_vatrachos",
                loc = "loc.rusty_pirate_thieving_chest_vatrachos",
                openLoc = "loc.rusty_pirate_thieving_chest_open",
                level = 33,
                xp = 900,
                teleport = null,
            )
            pirate(
                "dbrow.thieving_chest_tarnished_crown_jewel",
                loc = "loc.tarnished_pirate_thieving_chest_crown_jewel",
                openLoc = "loc.tarnished_pirate_thieving_chest_open",
                level = 54,
                xp = 1225,
                teleport = CoordGrid(1765, 2655),
            )
            pirate(
                "dbrow.thieving_chest_tarnished_shimmering_atoll",
                loc = "loc.tarnished_pirate_thieving_chest_shimmering_atoll",
                openLoc = "loc.tarnished_pirate_thieving_chest_open",
                level = 54,
                xp = 1225,
                teleport = CoordGrid(1550, 2774),
            )
            pirate(
                "dbrow.thieving_chest_reinforced_ynysdail",
                loc = "loc.reinforced_pirate_thieving_chest_ynysdail",
                openLoc = "loc.reinforced_pirate_thieving_chest_open",
                level = 76,
                xp = 1825,
                teleport = CoordGrid(2224, 3468),
            )
            pirate(
                "dbrow.thieving_chest_reinforced_brittle",
                loc = "loc.reinforced_pirate_thieving_chest_brittle",
                openLoc = "loc.reinforced_pirate_thieving_chest_open",
                level = 76,
                xp = 1825,
                teleport = CoordGrid(1956, 4060),
            )
        }
}

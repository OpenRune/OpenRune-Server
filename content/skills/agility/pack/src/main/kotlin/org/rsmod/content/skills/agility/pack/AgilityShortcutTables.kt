package org.rsmod.content.skills.agility.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

/** The gear a crossing needs, matching the constants content reads back out of the table. */
const val GEAR_NONE: Int = 0
const val GEAR_GRAPPLE: Int = 1
const val GEAR_CLIMBING_BOOTS: Int = 2

/**
 * A crossing that can go wrong: the wiki's level-1 and level-99 odds out of 256, the xp a fail still
 * pays, and the damage it deals.
 */
data class FailDecl(val low: Int, val high: Int, val xp: Double, val damage: IntRange? = null)

/**
 * One crossing of an obstacle. The level and the requirements sit on the link rather than the
 * shortcut because an obstacle can be two shortcuts wearing one loc id: both Catacombs of Kourend
 * cracks are `loc.zeah_cata_crack`, and the northern one wants seventeen more levels.
 *
 * [bareLevel] is the alternative live offers on the grapple crossings: the same gap, no crossbow, a
 * much higher Agility level.
 */
data class LinkDecl(
    val origin: CoordGrid,
    val dest: CoordGrid,
    val level: Int,
    val ranged: Int = 0,
    val strength: Int = 0,
    val gear: Int = GEAR_NONE,
    val quest: String? = null,
    val varSymbol: String? = null,
    val varValue: Int = 0,
    val varExact: Boolean = false,
    val bareLevel: Int = 0,
)

/** One obstacle, bound by the [option] the wiki lists for it rather than by the loc's first op. */
data class ShortcutDecl(
    val row: String,
    val loc: String,
    val option: String,
    val level: Int,
    val xp: Double,
    val ticks: Int,
    val links: List<LinkDecl>,
    val fail: FailDecl? = null,
)

/**
 * Agility shortcuts and the tiles they cross between.
 *
 * Tiles, levels and gates come from the shortest-path RuneLite plugin's `agility_shortcuts.tsv`
 * (https://github.com/Skretzo/shortest-path), BSD 2-Clause, Copyright (c) 2021 Skretzo. Xp is the
 * wiki's, matched by loc id.
 *
 * Xp is stored multiplied by ten, since the columns are ints and the wiki quotes halves.
 */
object AgilityShortcutTables {
    const val SC_LOC = 0
    const val SC_OPTION = 1
    const val SC_LEVEL = 2
    const val SC_XP = 3
    const val SC_TICKS = 4
    const val SC_FAIL = 5

    const val LINK_SHORTCUT = 0
    const val LINK_ORIGIN = 1
    const val LINK_DEST = 2
    const val LINK_LEVEL = 3
    const val LINK_RANGED = 4
    const val LINK_STRENGTH = 5
    const val LINK_GEAR = 6
    const val LINK_QUEST = 7
    const val LINK_VAR = 8
    const val LINK_BARE_LEVEL = 9

    fun shortcuts() =
        dbTable("dbtable.agility_shortcut", serverOnly = true) {
            column("loc", SC_LOC, VarType.LOC)
            column("option", SC_OPTION, VarType.STRING)
            column("level", SC_LEVEL, VarType.INT)
            column("xp", SC_XP, VarType.INT)
            column("ticks", SC_TICKS, VarType.INT)
            // low, high, the xp a fail still pays in tenths, damage min, damage max.
            column("fail", SC_FAIL, VarType.INT, VarType.INT, VarType.INT, VarType.INT, VarType.INT)

            for (shortcut in all) {
                row("dbrow.${shortcut.row}") {
                    columnRSCM(SC_LOC, shortcut.loc)
                    column(SC_OPTION, shortcut.option)
                    column(SC_LEVEL, shortcut.level)
                    column(SC_XP, (shortcut.xp * 10).toInt())
                    column(SC_TICKS, shortcut.ticks)
                    shortcut.fail?.let {
                        column(
                            SC_FAIL,
                            it.low,
                            it.high,
                            (it.xp * 10).toInt(),
                            it.damage?.first ?: 0,
                            it.damage?.last ?: 0,
                        )
                    }
                }
            }
        }

    fun shortcutLinks() =
        dbTable("dbtable.agility_shortcut_link", serverOnly = true) {
            column("shortcut", LINK_SHORTCUT, VarType.DBROW)
            column("origin", LINK_ORIGIN, VarType.COORDGRID)
            column("dest", LINK_DEST, VarType.COORDGRID)
            column("level", LINK_LEVEL, VarType.INT)
            column("ranged", LINK_RANGED, VarType.INT)
            column("strength", LINK_STRENGTH, VarType.INT)
            column("gear", LINK_GEAR, VarType.INT)
            column("quest", LINK_QUEST, VarType.STRING)
            // symbol, value, 1 when the comparison is exact rather than at-least.
            column("var_gate", LINK_VAR, VarType.STRING, VarType.INT, VarType.INT)
            column("bare_level", LINK_BARE_LEVEL, VarType.INT)

            var index = 0
            for (shortcut in all) {
                for (link in shortcut.links) {
                    row("dbrow.agility_link_%03d".format(index++)) {
                        columnRSCM(LINK_SHORTCUT, "dbrow.${shortcut.row}")
                        columnCoord(LINK_ORIGIN, link.origin)
                        columnCoord(LINK_DEST, link.dest)
                        column(LINK_LEVEL, link.level)
                        if (link.ranged > 0) column(LINK_RANGED, link.ranged)
                        if (link.strength > 0) column(LINK_STRENGTH, link.strength)
                        if (link.gear != GEAR_NONE) column(LINK_GEAR, link.gear)
                        link.quest?.let { column(LINK_QUEST, it) }
                        link.varSymbol?.let {
                            column(LINK_VAR, it, link.varValue, if (link.varExact) 1 else 0)
                        }
                        if (link.bareLevel > 0) column(LINK_BARE_LEVEL, link.bareLevel)
                    }
                }
            }
        }

    internal val all: List<ShortcutDecl> =
        listOf(

            ShortcutDecl(
                row = "agility_sc_hosidiusquest_rock_snake_climb",
                loc = "loc.hosidiusquest_rock_snake",
                option = "Climb",
                level = 1,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1686, 9755, 0),
                            dest = CoordGrid(1688, 9755, 0),
                            level = 1,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_karam_dungeon_bamboo_logbalance1_walk_across",
                loc = "loc.karam_dungeon_bamboo_logbalance1",
                option = "Walk-across",
                level = 1,
                xp = 0.0,
                ticks = 7,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2682, 9506, 0),
                            dest = CoordGrid(2687, 9506, 0),
                            level = 1,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_karam_dungeon_bamboo_logbalance3_walk_across",
                loc = "loc.karam_dungeon_bamboo_logbalance3",
                option = "Walk-across",
                level = 1,
                xp = 0.0,
                ticks = 7,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2687, 9506, 0),
                            dest = CoordGrid(2682, 9506, 0),
                            level = 1,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_karam_dungeon_pipe_squeeze_through",
                loc = "loc.karam_dungeon_pipe",
                option = "Squeeze-through",
                level = 1,
                xp = 0.0,
                ticks = 10,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2698, 9492, 0),
                            dest = CoordGrid(2698, 9500, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2698, 9500, 0),
                            dest = CoordGrid(2698, 9492, 0),
                            level = 1,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_karam_dungeon_pipe2_squeeze_through",
                loc = "loc.karam_dungeon_pipe2",
                option = "Squeeze-through",
                level = 1,
                xp = 8.5,
                ticks = 10,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2655, 9566, 0),
                            dest = CoordGrid(2655, 9573, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2655, 9573, 0),
                            dest = CoordGrid(2655, 9566, 0),
                            level = 1,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_karam_dungeon_stone1_jump_from",
                loc = "loc.karam_dungeon_stone1",
                option = "Jump-from",
                level = 1,
                xp = 7.5,
                ticks = 7,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2649, 9562, 0),
                            dest = CoordGrid(2647, 9557, 0),
                            level = 1,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_karam_dungeon_stone2_jump_from",
                loc = "loc.karam_dungeon_stone2",
                option = "Jump-from",
                level = 1,
                xp = 7.5,
                ticks = 7,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2647, 9557, 0),
                            dest = CoordGrid(2649, 9562, 0),
                            level = 1,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_regicide_pitfall_side_jump",
                loc = "loc.regicide_pitfall_side",
                option = "Jump",
                level = 1,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2209, 3201, 0),
                            dest = CoordGrid(2209, 3205, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2209, 3205, 0),
                            dest = CoordGrid(2209, 3201, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2267, 3201, 0),
                            dest = CoordGrid(2267, 3205, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2267, 3205, 0),
                            dest = CoordGrid(2267, 3201, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2274, 3172, 0),
                            dest = CoordGrid(2274, 3176, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2274, 3176, 0),
                            dest = CoordGrid(2274, 3172, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2275, 3262, 0),
                            dest = CoordGrid(2279, 3262, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2279, 3262, 0),
                            dest = CoordGrid(2275, 3262, 0),
                            level = 1,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_regicide_trap_tripwire_step_over",
                loc = "loc.regicide_trap_tripwire",
                option = "Step-over",
                level = 1,
                xp = 0.0,
                ticks = 8,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2215, 3153, 0),
                            dest = CoordGrid(2215, 3156, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2215, 3156, 0),
                            dest = CoordGrid(2215, 3153, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2220, 3152, 0),
                            dest = CoordGrid(2220, 3155, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2220, 3155, 0),
                            dest = CoordGrid(2220, 3152, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2250, 3168, 0),
                            dest = CoordGrid(2253, 3168, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2253, 3168, 0),
                            dest = CoordGrid(2250, 3168, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2284, 3188, 0),
                            dest = CoordGrid(2287, 3188, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2287, 3188, 0),
                            dest = CoordGrid(2284, 3188, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2294, 3242, 0),
                            dest = CoordGrid(2294, 3245, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2294, 3245, 0),
                            dest = CoordGrid(2294, 3242, 0),
                            level = 1,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_regicide_trap_woodspring_pass",
                loc = "loc.regicide_trap_woodspring",
                option = "Pass",
                level = 1,
                xp = 0.0,
                ticks = 6,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2181, 3208, 0),
                            dest = CoordGrid(2181, 3212, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2181, 3212, 0),
                            dest = CoordGrid(2181, 3208, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2199, 3169, 0),
                            dest = CoordGrid(2202, 3169, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2202, 3169, 0),
                            dest = CoordGrid(2199, 3169, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2234, 3181, 0),
                            dest = CoordGrid(2238, 3181, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2238, 3181, 0),
                            dest = CoordGrid(2234, 3181, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2256, 3227, 0),
                            dest = CoordGrid(2260, 3227, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2260, 3227, 0),
                            dest = CoordGrid(2256, 3227, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2274, 3163, 0),
                            dest = CoordGrid(2277, 3163, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2277, 3163, 0),
                            dest = CoordGrid(2274, 3163, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2295, 3213, 0),
                            dest = CoordGrid(2295, 3217, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2295, 3217, 0),
                            dest = CoordGrid(2295, 3213, 0),
                            level = 1,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_swamp_cave_steppingstone_a_jump_across",
                loc = "loc.swamp_cave_steppingstone_a",
                option = "Jump-across",
                level = 1,
                xp = 3.0,
                ticks = 4,
                fail = FailDecl(51, 252, 1.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3204, 9572, 0),
                            dest = CoordGrid(3208, 9572, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3208, 9572, 0),
                            dest = CoordGrid(3204, 9572, 0),
                            level = 1,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_swamp_cave_steppingstone_b_jump_across",
                loc = "loc.swamp_cave_steppingstone_b",
                option = "Jump-across",
                level = 1,
                xp = 3.0,
                ticks = 4,
                fail = FailDecl(51, 252, 1.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3221, 9552, 0),
                            dest = CoordGrid(3221, 9556, 0),
                            level = 1,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3221, 9556, 0),
                            dest = CoordGrid(3221, 9552, 0),
                            level = 1,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_xbows_fai_falador_castle_walls_crenalation_jump",
                loc = "loc.xbows_fai_falador_castle_walls_crenalation",
                option = "Jump",
                level = 4,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3032, 3389, 1),
                            dest = CoordGrid(3032, 3388, 0),
                            level = 4,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_xbows_fai_falador_castle_walls_battlement_jump",
                loc = "loc.xbows_fai_falador_castle_walls_battlement",
                option = "Jump",
                level = 4,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3033, 3389, 1),
                            dest = CoordGrid(3033, 3390, 0),
                            level = 4,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_xbows_yanille_castlewall_battlement_jump",
                loc = "loc.xbows_yanille_castlewall_battlement",
                option = "Jump",
                level = 4,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2556, 3073, 1),
                            dest = CoordGrid(2556, 3072, 0),
                            level = 4,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2556, 3074, 1),
                            dest = CoordGrid(2556, 3075, 0),
                            level = 4,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_fai_falador_castle_crumble_mid_climb_over",
                loc = "loc.fai_falador_castle_crumble_mid",
                option = "Climb-over",
                level = 5,
                xp = 0.5,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2934, 3355, 0),
                            dest = CoordGrid(2936, 3355, 0),
                            level = 5,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2936, 3355, 0),
                            dest = CoordGrid(2934, 3355, 0),
                            level = 5,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_watchshortcut_climb_up",
                loc = "loc.watchshortcut",
                option = "Climb-up",
                level = 5,
                xp = 25.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2561, 3108, 0),
                            dest = CoordGrid(2561, 3111, 0),
                            level = 5,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_xbows_raft_br_grapple",
                loc = "loc.xbows_raft_br",
                option = "Grapple",
                level = 8,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3246, 3179, 0),
                            dest = CoordGrid(3259, 3179, 0),
                            level = 8,
                            ranged = 37,
                            strength = 19,
                            gear = GEAR_GRAPPLE,
                            bareLevel = 48,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3259, 3179, 0),
                            dest = CoordGrid(3246, 3179, 0),
                            level = 8,
                            ranged = 37,
                            strength = 19,
                            gear = GEAR_GRAPPLE,
                            bareLevel = 48,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_ds2_corsair_cove_shortcut_climb",
                loc = "loc.ds2_corsair_cove_shortcut",
                option = "Climb",
                level = 10,
                xp = 1.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2546, 2871, 0),
                            dest = CoordGrid(2546, 2873, 0),
                            level = 10,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2546, 2873, 0),
                            dest = CoordGrid(2546, 2871, 0),
                            level = 10,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_xbows_fai_falador_castle_arches_hillskew_grapple",
                loc = "loc.xbows_fai_falador_castle_arches_hillskew",
                option = "Grapple",
                level = 11,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3032, 3388, 0),
                            dest = CoordGrid(3032, 3389, 1),
                            level = 11,
                            ranged = 19,
                            strength = 37,
                            gear = GEAR_GRAPPLE,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_xbows_fai_falador_castle_walls_hillskew_grapple",
                loc = "loc.xbows_fai_falador_castle_walls_hillskew",
                option = "Grapple",
                level = 11,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3033, 3390, 0),
                            dest = CoordGrid(3033, 3389, 1),
                            level = 11,
                            ranged = 19,
                            strength = 37,
                            gear = GEAR_GRAPPLE,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_lost_tribe_cavewall_hole_walldecor_squeeze_through",
                loc = "loc.lost_tribe_cavewall_hole_walldecor",
                option = "Squeeze-through",
                level = 13,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3219, 9618, 0),
                            dest = CoordGrid(3221, 9618, 0),
                            level = 13,
                            quest = "quest_losttribe",
                        ),
                        LinkDecl(
                            origin = CoordGrid(3221, 9618, 0),
                            dest = CoordGrid(3219, 9618, 0),
                            level = 13,
                            quest = "quest_losttribe",
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_lumbridge_sc_fencejump_jump_over",
                loc = "loc.lumbridge_sc_fencejump",
                option = "Jump-over",
                level = 13,
                xp = 0.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3240, 3334, 0),
                            dest = CoordGrid(3240, 3335, 0),
                            level = 13,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3240, 3335, 0),
                            dest = CoordGrid(3240, 3334, 0),
                            level = 13,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_burthorpe_diary_shortcut_manoeuvre_past",
                loc = "loc.burthorpe_diary_shortcut",
                option = "Manoeuvre-past",
                level = 14,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2926, 3522, 0),
                            dest = CoordGrid(2928, 3520, 0),
                            level = 14,
                            varSymbol = "varbit.falador_diary_easy_complete",
                            varValue = 1,
                            varExact = true,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2927, 3523, 0),
                            dest = CoordGrid(2928, 3520, 0),
                            level = 14,
                            varSymbol = "varbit.falador_diary_easy_complete",
                            varValue = 1,
                            varExact = true,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2928, 3520, 0),
                            dest = CoordGrid(2927, 3523, 0),
                            level = 14,
                            varSymbol = "varbit.falador_diary_easy_complete",
                            varValue = 1,
                            varExact = true,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_ds2_ogre_corsair_dungeon_shortcut_jump_to",
                loc = "loc.ds2_ogre_corsair_dungeon_shortcut",
                option = "Jump-to",
                level = 15,
                xp = 2.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1981, 8994, 1),
                            dest = CoordGrid(1981, 8998, 1),
                            level = 15,
                            varSymbol = "varbit.corsair_cove_resource_entry",
                            varValue = 1,
                            varExact = true,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1981, 8998, 1),
                            dest = CoordGrid(1981, 8994, 1),
                            level = 15,
                            varSymbol = "varbit.corsair_cove_resource_entry",
                            varValue = 1,
                            varExact = true,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1981, 8999, 1),
                            dest = CoordGrid(1981, 8994, 1),
                            level = 15,
                            varSymbol = "varbit.corsair_cove_resource_entry",
                            varValue = 1,
                            varExact = true,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_troll_climbingrocks_climb",
                loc = "loc.troll_climbingrocks",
                option = "Climb",
                level = 15,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2820, 3635, 0),
                            dest = CoordGrid(2822, 3635, 0),
                            level = 15,
                            gear = GEAR_CLIMBING_BOOTS,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2856, 3611, 0),
                            dest = CoordGrid(2856, 3613, 0),
                            level = 15,
                            gear = GEAR_CLIMBING_BOOTS,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2857, 3611, 0),
                            dest = CoordGrid(2857, 3613, 0),
                            level = 15,
                            gear = GEAR_CLIMBING_BOOTS,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_zqclimbingrocks_climb",
                loc = "loc.zqclimbingrocks",
                option = "Climb",
                level = 15,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2791, 2978, 0),
                            dest = CoordGrid(2795, 2978, 0),
                            level = 15,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2791, 2979, 0),
                            dest = CoordGrid(2795, 2979, 0),
                            level = 15,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2791, 2980, 0),
                            dest = CoordGrid(2795, 2980, 0),
                            level = 15,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2795, 2978, 0),
                            dest = CoordGrid(2791, 2978, 0),
                            level = 15,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2795, 2979, 0),
                            dest = CoordGrid(2791, 2979, 0),
                            level = 15,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2795, 2980, 0),
                            dest = CoordGrid(2791, 2980, 0),
                            level = 15,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_yanille_castlehole_sc_climb_into",
                loc = "loc.yanille_castlehole_sc",
                option = "Climb-into",
                level = 16,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2575, 3112, 0),
                            dest = CoordGrid(2575, 3107, 0),
                            level = 16,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_zeah_cata_crack_squeeze_through",
                loc = "loc.zeah_cata_crack",
                option = "Squeeze-through",
                level = 17,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1645, 10000, 0),
                            dest = CoordGrid(1647, 10010, 0),
                            level = 17,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1647, 10010, 0),
                            dest = CoordGrid(1645, 10000, 0),
                            level = 17,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1706, 10078, 0),
                            dest = CoordGrid(1716, 10056, 0),
                            level = 34,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1716, 10056, 0),
                            dest = CoordGrid(1706, 10078, 0),
                            level = 34,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_dugupsoil_slayer_2_climb_through",
                loc = "loc.dugupsoil_slayer_2",
                option = "Climb-through",
                level = 18,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3442, 3531, 0),
                            dest = CoordGrid(3444, 3533, 0),
                            level = 18,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3444, 3533, 0),
                            dest = CoordGrid(3442, 3531, 0),
                            level = 18,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3420, 3534, 2),
                            dest = CoordGrid(3418, 3532, 0),
                            level = 81,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_gargboss_healsphere_med_climb",
                loc = "loc.gargboss_healsphere_med",
                option = "Climb",
                level = 18,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1687, 9801, 0),
                            dest = CoordGrid(1689, 9801, 0),
                            level = 18,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1689, 9801, 0),
                            dest = CoordGrid(1687, 9801, 0),
                            level = 18,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_hosidiusquest_stone_cross",
                loc = "loc.hosidiusquest_stone",
                option = "Cross",
                level = 18,
                xp = 0.0,
                ticks = 8,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1702, 9800, 0),
                            dest = CoordGrid(1708, 9800, 0),
                            level = 18,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1708, 9800, 0),
                            dest = CoordGrid(1702, 9800, 0),
                            level = 18,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_qip_watchtower_trellis_base_climb_up",
                loc = "loc.qip_watchtower_trellis_base",
                option = "Climb-up",
                level = 18,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2548, 3119, 0),
                            dest = CoordGrid(2548, 3117, 1),
                            level = 18,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_slayertower_window_shortcut_through_climb_through",
                loc = "loc.slayertower_window_shortcut_through",
                option = "Climb-through",
                level = 18,
                xp = 3.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3442, 3531, 0),
                            dest = CoordGrid(3444, 3533, 0),
                            level = 18,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3444, 3533, 0),
                            dest = CoordGrid(3442, 3531, 0),
                            level = 18,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_mine_log_balance1_walk_across",
                loc = "loc.mine_log_balance1",
                option = "Walk-across",
                level = 20,
                xp = 8.5,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2598, 3477, 0),
                            dest = CoordGrid(2603, 3477, 0),
                            level = 20,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2599, 3476, 0),
                            dest = CoordGrid(2603, 3477, 0),
                            level = 20,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2599, 3478, 0),
                            dest = CoordGrid(2603, 3477, 0),
                            level = 20,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2602, 3476, 0),
                            dest = CoordGrid(2598, 3477, 0),
                            level = 20,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2602, 3478, 0),
                            dest = CoordGrid(2598, 3477, 0),
                            level = 20,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2603, 3477, 0),
                            dest = CoordGrid(2598, 3477, 0),
                            level = 20,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_varrock_sc_tunnel_east_climb_into",
                loc = "loc.varrock_sc_tunnel_east",
                option = "Climb-into",
                level = 21,
                xp = 0.0,
                ticks = 7,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3142, 3513, 0),
                            dest = CoordGrid(3137, 3516, 0),
                            level = 21,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_varrock_sc_tunnel_west_climb_into",
                loc = "loc.varrock_sc_tunnel_west",
                option = "Climb-into",
                level = 21,
                xp = 0.0,
                ticks = 7,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3137, 3516, 0),
                            dest = CoordGrid(3142, 3513, 0),
                            level = 21,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_xbows_rock_grappled_grapple",
                loc = "loc.xbows_rock_grappled",
                option = "Grapple",
                level = 23,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2449, 3155, 0),
                            dest = CoordGrid(2444, 3165, 0),
                            level = 23,
                            ranged = 24,
                            strength = 28,
                            varSymbol = "varbit.observatory_shortcut_rope",
                            varValue = 1,
                            varExact = true,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_xbows_rope_diagonal_obs_climb",
                loc = "loc.xbows_rope_diagonal_obs",
                option = "Climb",
                level = 23,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2449, 3155, 0),
                            dest = CoordGrid(2444, 3165, 0),
                            level = 23,
                            ranged = 24,
                            strength = 28,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_av_lowwall_climb_1_climb_over",
                loc = "loc.av_lowwall_climb_1",
                option = "Climb-over",
                level = 24,
                xp = 6.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1369, 3294, 0),
                            dest = CoordGrid(1369, 3296, 0),
                            level = 24,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1369, 3296, 0),
                            dest = CoordGrid(1369, 3294, 0),
                            level = 24,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1387, 3301, 0),
                            dest = CoordGrid(1387, 3303, 0),
                            level = 24,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1387, 3303, 0),
                            dest = CoordGrid(1387, 3301, 0),
                            level = 24,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_av_lowwall_climb_2_climb_over",
                loc = "loc.av_lowwall_climb_2",
                option = "Climb-over",
                level = 24,
                xp = 6.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1389, 3309, 0),
                            dest = CoordGrid(1391, 3309, 0),
                            level = 24,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1391, 3309, 0),
                            dest = CoordGrid(1389, 3309, 0),
                            level = 24,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_ep_climbing_rocks01_climb",
                loc = "loc.ep_climbing_rocks01",
                option = "Climb",
                level = 25,
                xp = 1.0,
                ticks = 6,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2322, 3502, 0),
                            dest = CoordGrid(2324, 3497, 0),
                            level = 25,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2324, 3497, 0),
                            dest = CoordGrid(2322, 3502, 0),
                            level = 25,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_falador_sc_castlewall_north_climb_into",
                loc = "loc.falador_sc_castlewall_north",
                option = "Climb-into",
                level = 26,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2948, 3313, 0),
                            dest = CoordGrid(2948, 3309, 0),
                            level = 26,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_falador_sc_castlewall_south_climb_into",
                loc = "loc.falador_sc_castlewall_south",
                option = "Climb-into",
                level = 26,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2948, 3309, 0),
                            dest = CoordGrid(2948, 3313, 0),
                            level = 26,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_zeah_cata_stepstone_jump_to",
                loc = "loc.zeah_cata_stepstone",
                option = "Jump-to",
                level = 28,
                xp = 2.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1609, 10061, 0),
                            dest = CoordGrid(1613, 10070, 0),
                            level = 28,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1613, 10070, 0),
                            dest = CoordGrid(1609, 10061, 0),
                            level = 28,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_mount_karuulm_shortcut_rocks_low_climb",
                loc = "loc.mount_karuulm_shortcut_rocks_low",
                option = "Climb",
                level = 29,
                xp = 0.0,
                ticks = 8,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1324, 3777, 0),
                            dest = CoordGrid(1324, 3785, 0),
                            level = 29,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1324, 3785, 0),
                            dest = CoordGrid(1324, 3777, 0),
                            level = 29,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_ds2_corsair_shortcut_bottom_climb_down",
                loc = "loc.ds2_corsair_shortcut_bottom",
                option = "Climb-down",
                level = 30,
                xp = 2.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2489, 2898, 0),
                            dest = CoordGrid(2485, 2898, 0),
                            level = 30,
                            quest = "quest_dragonslayer1",
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_ds2_corsair_shortcut_top_climb_down",
                loc = "loc.ds2_corsair_shortcut_top",
                option = "Climb-down",
                level = 30,
                xp = 2.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2485, 2898, 0),
                            dest = CoordGrid(2489, 2898, 0),
                            level = 30,
                            quest = "quest_dragonslayer1",
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_zqrockjump1_cross",
                loc = "loc.zqrockjump1",
                option = "Cross",
                level = 30,
                xp = 3.0,
                ticks = 2,
                fail = FailDecl(50, 253, 1.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2925, 2949, 0),
                            dest = CoordGrid(2925, 2950, 0),
                            level = 30,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2925, 2951, 0),
                            dest = CoordGrid(2925, 2950, 0),
                            level = 30,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_zqrockjump2_cross",
                loc = "loc.zqrockjump2",
                option = "Cross",
                level = 30,
                xp = 3.0,
                ticks = 2,
                fail = FailDecl(50, 253, 1.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2925, 2948, 0),
                            dest = CoordGrid(2925, 2949, 0),
                            level = 30,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2925, 2950, 0),
                            dest = CoordGrid(2925, 2949, 0),
                            level = 30,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_zqrockjump3_cross",
                loc = "loc.zqrockjump3",
                option = "Cross",
                level = 30,
                xp = 3.0,
                ticks = 2,
                fail = FailDecl(50, 253, 1.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2925, 2947, 0),
                            dest = CoordGrid(2925, 2948, 0),
                            level = 30,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2925, 2949, 0),
                            dest = CoordGrid(2925, 2948, 0),
                            level = 30,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_lumbridge_sc_stepstone_jump_onto",
                loc = "loc.lumbridge_sc_stepstone",
                option = "Jump-onto",
                level = 31,
                xp = 3.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3149, 3363, 0),
                            dest = CoordGrid(3150, 3363, 0),
                            level = 31,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3150, 3363, 0),
                            dest = CoordGrid(3151, 3363, 0),
                            level = 31,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3151, 3363, 0),
                            dest = CoordGrid(3152, 3363, 0),
                            level = 31,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3152, 3363, 0),
                            dest = CoordGrid(3153, 3363, 0),
                            level = 31,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3153, 3363, 0),
                            dest = CoordGrid(3152, 3363, 0),
                            level = 31,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3154, 3363, 0),
                            dest = CoordGrid(3153, 3363, 0),
                            level = 31,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_shilo_river_steppingstone_cross",
                loc = "loc.shilo_river_steppingstone",
                option = "Cross",
                level = 32,
                xp = 0.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2863, 2971, 0),
                            dest = CoordGrid(2863, 2976, 0),
                            level = 32,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2863, 2976, 0),
                            dest = CoordGrid(2863, 2971, 0),
                            level = 32,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_xbows_rock_hilltop_basic_grapple",
                loc = "loc.xbows_rock_hilltop_basic",
                option = "Grapple",
                level = 32,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2866, 3428, 0),
                            dest = CoordGrid(2869, 3428, 0),
                            level = 32,
                            ranged = 35,
                            strength = 35,
                            gear = GEAR_GRAPPLE,
                            bareLevel = 68,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_ardougne_log_balance_left_sc_walk_across",
                loc = "loc.ardougne_log_balance_left_sc",
                option = "Walk-across",
                level = 33,
                xp = 4.0,
                ticks = 3,
                fail = FailDecl(90, 250, 2.0, 2..6),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2598, 3336, 0),
                            dest = CoordGrid(2602, 3336, 0),
                            level = 33,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2599, 3337, 0),
                            dest = CoordGrid(2602, 3336, 0),
                            level = 33,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_ardougne_log_balance_right_sc_walk_across",
                loc = "loc.ardougne_log_balance_right_sc",
                option = "Walk-across",
                level = 33,
                xp = 4.0,
                ticks = 3,
                fail = FailDecl(90, 250, 2.0, 2..6),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2602, 3336, 0),
                            dest = CoordGrid(2598, 3336, 0),
                            level = 33,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_av_tunnel_1_climb_into",
                loc = "loc.av_tunnel_1",
                option = "Climb-into",
                level = 33,
                xp = 7.5,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1368, 3323, 0),
                            dest = CoordGrid(1368, 3327, 0),
                            level = 33,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1368, 3327, 0),
                            dest = CoordGrid(1368, 3323, 0),
                            level = 33,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_agility_obstical_pipe_barbarian_squeeze_through",
                loc = "loc.agility_obstical_pipe_barbarian",
                option = "Squeeze-through",
                level = 35,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2552, 3558, 0),
                            dest = CoordGrid(2552, 3561, 0),
                            level = 35,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2552, 3561, 0),
                            dest = CoordGrid(2552, 3558, 0),
                            level = 35,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_garden_trellis_concave_shortcut_climb",
                loc = "loc.garden_trellis_concave_shortcut",
                option = "Climb",
                level = 35,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3228, 3470, 0),
                            dest = CoordGrid(3228, 3471, 0),
                            level = 35,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_av_stepstone_1_cross",
                loc = "loc.av_stepstone_1",
                option = "Cross",
                level = 36,
                xp = 0.0,
                ticks = 7,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1393, 3309, 0),
                            dest = CoordGrid(1399, 3309, 0),
                            level = 36,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1399, 3309, 0),
                            dest = CoordGrid(1393, 3309, 0),
                            level = 36,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_xbows_beach_to_island_tree_basic_grapple",
                loc = "loc.xbows_beach_to_island_tree_basic",
                option = "Grapple",
                level = 36,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2841, 3427, 0),
                            dest = CoordGrid(2841, 3433, 0),
                            level = 36,
                            ranged = 39,
                            strength = 22,
                            gear = GEAR_GRAPPLE,
                            bareLevel = 72,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_gnome_stronghold_sc_rock_bottom_climb",
                loc = "loc.gnome_stronghold_sc_rock_bottom",
                option = "Climb",
                level = 37,
                xp = 0.0,
                ticks = 9,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2489, 3521, 0),
                            dest = CoordGrid(2486, 3515, 0),
                            level = 37,
                            quest = "quest_grandtree",
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_gnome_stronghold_sc_rock_top_climb",
                loc = "loc.gnome_stronghold_sc_rock_top",
                option = "Climb",
                level = 37,
                xp = 0.0,
                ticks = 9,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2486, 3515, 0),
                            dest = CoordGrid(2489, 3521, 0),
                            level = 37,
                            quest = "quest_grandtree",
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_alkharid_mine_sc_bottom_climb",
                loc = "loc.alkharid_mine_sc_bottom",
                option = "Climb",
                level = 38,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3302, 3315, 0),
                            dest = CoordGrid(3306, 3315, 0),
                            level = 38,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_alkharid_mine_sc_top_climb",
                loc = "loc.alkharid_mine_sc_top",
                option = "Climb",
                level = 38,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3306, 3315, 0),
                            dest = CoordGrid(3302, 3315, 0),
                            level = 38,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_xbows_yanille_castlewall_grapple",
                loc = "loc.xbows_yanille_castlewall",
                option = "Grapple",
                level = 39,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2556, 3072, 0),
                            dest = CoordGrid(2556, 3073, 1),
                            level = 39,
                            ranged = 21,
                            strength = 38,
                            gear = GEAR_GRAPPLE,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2556, 3075, 0),
                            dest = CoordGrid(2556, 3074, 1),
                            level = 39,
                            ranged = 21,
                            strength = 38,
                            gear = GEAR_GRAPPLE,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_balancing_ledge3_walk_across",
                loc = "loc.balancing_ledge3",
                option = "Walk-across",
                level = 40,
                xp = 0.0,
                ticks = 9,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2580, 9512, 0),
                            dest = CoordGrid(2580, 9520, 0),
                            level = 40,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2580, 9520, 0),
                            dest = CoordGrid(2580, 9512, 0),
                            level = 40,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_obstical_pipe4_squeeze_through",
                loc = "loc.obstical_pipe4",
                option = "Squeeze-through",
                level = 40,
                xp = 0.0,
                ticks = 10,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2578, 9506, 0),
                            dest = CoordGrid(2572, 9506, 0),
                            level = 40,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_tlati_north_river_log_balance_1_walk_across",
                loc = "loc.tlati_north_river_log_balance_1",
                option = "Walk-across",
                level = 40,
                xp = 0.0,
                ticks = 10,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1283, 3138, 0),
                            dest = CoordGrid(1283, 3147, 0),
                            level = 40,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1283, 3147, 0),
                            dest = CoordGrid(1283, 3138, 0),
                            level = 40,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_zeah_lake_shortcut_hosidius_cross",
                loc = "loc.zeah_lake_shortcut_hosidius",
                option = "Cross",
                level = 40,
                xp = 0.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1610, 3570, 0),
                            dest = CoordGrid(1614, 3570, 0),
                            level = 40,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1614, 3570, 0),
                            dest = CoordGrid(1610, 3570, 0),
                            level = 40,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_zeah_lake_shortcut_shayzien_cross",
                loc = "loc.zeah_lake_shortcut_shayzien",
                option = "Cross",
                level = 40,
                xp = 0.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1603, 3571, 0),
                            dest = CoordGrid(1607, 3571, 0),
                            level = 40,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1607, 3571, 0),
                            dest = CoordGrid(1603, 3571, 0),
                            level = 40,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_asc_troll_mountain_climbrock_1_climb",
                loc = "loc.asc_troll_mountain_climbrock_1",
                option = "Climb",
                level = 41,
                xp = 0.0,
                ticks = 3,
                fail = FailDecl(170, 253, 0.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2869, 3671, 0),
                            dest = CoordGrid(2872, 3671, 0),
                            level = 41,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2872, 3671, 0),
                            dest = CoordGrid(2869, 3671, 0),
                            level = 41,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_av_scramble_1_climb",
                loc = "loc.av_scramble_1",
                option = "Climb",
                level = 41,
                xp = 9.5,
                ticks = 6,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1391, 3323, 0),
                            dest = CoordGrid(1396, 3323, 0),
                            level = 41,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1396, 3323, 0),
                            dest = CoordGrid(1391, 3323, 0),
                            level = 41,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_draynor_diary_under_wall_e_climb_into",
                loc = "loc.draynor_diary_under_wall_e",
                option = "Climb-into",
                level = 42,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3070, 3257, 0),
                            dest = CoordGrid(3066, 3257, 0),
                            level = 42,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_draynor_diary_under_wall_w_climb_into",
                loc = "loc.draynor_diary_under_wall_w",
                option = "Climb-into",
                level = 42,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3066, 3257, 0),
                            dest = CoordGrid(3070, 3257, 0),
                            level = 42,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_dwarf_mines_sc_wall_crack_squeeze_through",
                loc = "loc.dwarf_mines_sc_wall_crack",
                option = "Squeeze-through",
                level = 42,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3028, 9806, 0),
                            dest = CoordGrid(3035, 9806, 0),
                            level = 42,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3035, 9806, 0),
                            dest = CoordGrid(3028, 9806, 0),
                            level = 42,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_asc_troll_mountain_climbrock_2_climb",
                loc = "loc.asc_troll_mountain_climbrock_2",
                option = "Climb",
                level = 43,
                xp = 0.0,
                ticks = 3,
                fail = FailDecl(170, 253, 0.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2877, 3666, 0),
                            dest = CoordGrid(2878, 3668, 0),
                            level = 43,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2878, 3665, 0),
                            dest = CoordGrid(2878, 3668, 0),
                            level = 43,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2878, 3668, 0),
                            dest = CoordGrid(2878, 3665, 0),
                            level = 43,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2879, 3667, 0),
                            dest = CoordGrid(2878, 3665, 0),
                            level = 43,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_tlati_tree_area_shortcut_top_climb",
                loc = "loc.tlati_tree_area_shortcut_top",
                option = "Climb",
                level = 43,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1270, 3002, 0),
                            dest = CoordGrid(1274, 3002, 0),
                            level = 43,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1274, 3002, 0),
                            dest = CoordGrid(1270, 3002, 0),
                            level = 43,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_troll_mountain_shortcut_climbingrocks1_climb",
                loc = "loc.troll_mountain_shortcut_climbingrocks1",
                option = "Climb",
                level = 43,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2884, 3683, 0),
                            dest = CoordGrid(2886, 3683, 0),
                            level = 43,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2886, 3683, 0),
                            dest = CoordGrid(2884, 3683, 0),
                            level = 43,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_troll_mountain_shortcut_climbingrocks2_climb",
                loc = "loc.troll_mountain_shortcut_climbingrocks2",
                option = "Climb",
                level = 43,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2884, 3684, 0),
                            dest = CoordGrid(2886, 3684, 0),
                            level = 43,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2886, 3684, 0),
                            dest = CoordGrid(2884, 3684, 0),
                            level = 43,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_asc_troll_mountain_climbrock_3_climb",
                loc = "loc.asc_troll_mountain_climbrock_3",
                option = "Climb",
                level = 44,
                xp = 0.0,
                ticks = 3,
                fail = FailDecl(170, 253, 0.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2907, 3682, 0),
                            dest = CoordGrid(2909, 3684, 0),
                            level = 44,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2909, 3684, 0),
                            dest = CoordGrid(2907, 3682, 0),
                            level = 44,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_av_balance_1_walk_across",
                loc = "loc.av_balance_1",
                option = "Walk-across",
                level = 45,
                xp = 0.0,
                ticks = 9,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1401, 3283, 0),
                            dest = CoordGrid(1401, 3291, 0),
                            level = 45,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1401, 3291, 0),
                            dest = CoordGrid(1401, 3283, 0),
                            level = 45,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_av_balance_2_walk_across",
                loc = "loc.av_balance_2",
                option = "Walk-across",
                level = 45,
                xp = 19.5,
                ticks = 8,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1453, 3329, 0),
                            dest = CoordGrid(1453, 3336, 0),
                            level = 45,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1453, 3336, 0),
                            dest = CoordGrid(1453, 3329, 0),
                            level = 45,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_proudspire_climbing_rocks_dark01_op_climb",
                loc = "loc.proudspire_climbing_rocks_dark01_op",
                option = "Climb",
                level = 45,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1586, 3261, 0),
                            dest = CoordGrid(1591, 3257, 0),
                            level = 45,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1587, 3262, 0),
                            dest = CoordGrid(1591, 3257, 0),
                            level = 45,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1590, 3257, 0),
                            dest = CoordGrid(1586, 3262, 0),
                            level = 45,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1591, 3258, 0),
                            dest = CoordGrid(1586, 3262, 0),
                            level = 45,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_regicide_logbalance1_start_cross",
                loc = "loc.regicide_logbalance1_start",
                option = "Cross",
                level = 45,
                xp = 0.0,
                ticks = 8,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2196, 3237, 0),
                            dest = CoordGrid(2202, 3237, 0),
                            level = 45,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2202, 3237, 0),
                            dest = CoordGrid(2196, 3237, 0),
                            level = 45,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_regicide_logbalance2_start_cross",
                loc = "loc.regicide_logbalance2_start",
                option = "Cross",
                level = 45,
                xp = 0.0,
                ticks = 8,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2258, 3250, 0),
                            dest = CoordGrid(2264, 3250, 0),
                            level = 45,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2264, 3250, 0),
                            dest = CoordGrid(2258, 3250, 0),
                            level = 45,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_regicide_logbalance3_start_cross",
                loc = "loc.regicide_logbalance3_start",
                option = "Cross",
                level = 45,
                xp = 0.0,
                ticks = 9,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2290, 3232, 0),
                            dest = CoordGrid(2290, 3239, 0),
                            level = 45,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2290, 3239, 0),
                            dest = CoordGrid(2290, 3232, 0),
                            level = 45,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_zeah_saltpetre_shortcut_cross",
                loc = "loc.zeah_saltpetre_shortcut",
                option = "Cross",
                level = 45,
                xp = 0.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1720, 3551, 0),
                            dest = CoordGrid(1724, 3551, 0),
                            level = 45,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1724, 3551, 0),
                            dest = CoordGrid(1720, 3551, 0),
                            level = 45,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_fairy_sc_juttingwall_squeeze_past",
                loc = "loc.fairy_sc_juttingwall",
                option = "Squeeze-past",
                level = 46,
                xp = 0.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2400, 4402, 0),
                            dest = CoordGrid(2400, 4404, 0),
                            level = 46,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2400, 4404, 0),
                            dest = CoordGrid(2400, 4402, 0),
                            level = 46,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2409, 4400, 0),
                            dest = CoordGrid(2409, 4402, 0),
                            level = 66,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2409, 4402, 0),
                            dest = CoordGrid(2409, 4400, 0),
                            level = 66,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_asc_troll_mountain_climbrock_4_climb",
                loc = "loc.asc_troll_mountain_climbrock_4",
                option = "Climb",
                level = 47,
                xp = 8.0,
                ticks = 3,
                fail = FailDecl(170, 253, 0.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2900, 3680, 0),
                            dest = CoordGrid(2903, 3680, 0),
                            level = 47,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2901, 3679, 0),
                            dest = CoordGrid(2903, 3680, 0),
                            level = 47,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2902, 3681, 0),
                            dest = CoordGrid(2900, 3680, 0),
                            level = 47,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2903, 3680, 0),
                            dest = CoordGrid(2900, 3680, 0),
                            level = 47,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_ralos_rise_sc_climb",
                loc = "loc.ralos_rise_sc",
                option = "Climb",
                level = 47,
                xp = 0.0,
                ticks = 8,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1455, 3128, 0),
                            dest = CoordGrid(1465, 3128, 0),
                            level = 47,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1465, 3128, 0),
                            dest = CoordGrid(1455, 3128, 0),
                            level = 47,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_snakeboss_crate_cross",
                loc = "loc.snakeboss_crate",
                option = "Cross",
                level = 47,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2449, 3155, 0),
                            dest = CoordGrid(2444, 3165, 0),
                            level = 47,
                            quest = "quest_regicide",
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_slayer_river_sc_logbalance_1_walk_across",
                loc = "loc.slayer_river_sc_logbalance_1",
                option = "Walk-across",
                level = 48,
                xp = 4.0,
                ticks = 4,
                fail = FailDecl(140, 255, 0.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2722, 3596, 0),
                            dest = CoordGrid(2722, 3592, 0),
                            level = 48,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_slayer_river_sc_logbalance_3_walk_across",
                loc = "loc.slayer_river_sc_logbalance_3",
                option = "Walk-across",
                level = 48,
                xp = 4.0,
                ticks = 4,
                fail = FailDecl(140, 255, 0.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2722, 3592, 0),
                            dest = CoordGrid(2722, 3596, 0),
                            level = 48,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_archeuus_runestone_shortcut_boulder_jump",
                loc = "loc.archeuus_runestone_shortcut_boulder",
                option = "Jump",
                level = 49,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1776, 3884, 0),
                            dest = CoordGrid(1776, 3880, 0),
                            level = 49,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_crumbled_wall_jump_over",
                loc = "loc.crumbled_wall",
                option = "Jump-over",
                level = 50,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2789, 9296, 0),
                            dest = CoordGrid(2790, 9295, 0),
                            level = 50,
                            quest = "quest_legends",
                        ),
                        LinkDecl(
                            origin = CoordGrid(2790, 9295, 0),
                            dest = CoordGrid(2789, 9296, 0),
                            level = 50,
                            quest = "quest_legends",
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_fairy_island_nature_grotto_shortcut_cross",
                loc = "loc.fairy_island_nature_grotto_shortcut",
                option = "Cross",
                level = 50,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3417, 3325, 0),
                            dest = CoordGrid(3422, 3325, 0),
                            level = 50,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3422, 3325, 0),
                            dest = CoordGrid(3417, 3325, 0),
                            level = 50,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_great_conch_cliff_shortcut_town_bottom_climb",
                loc = "loc.great_conch_cliff_shortcut_town_bottom",
                option = "Climb",
                level = 50,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3180, 2432, 0),
                            dest = CoordGrid(3180, 2434, 0),
                            level = 50,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_great_conch_cliff_shortcut_town_top_climb",
                loc = "loc.great_conch_cliff_shortcut_town_top",
                option = "Climb",
                level = 50,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3180, 2434, 0),
                            dest = CoordGrid(3180, 2432, 0),
                            level = 50,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_varrock_dungeon_pipe_sc_squeeze_through",
                loc = "loc.varrock_dungeon_pipe_sc",
                option = "Squeeze-through",
                level = 51,
                xp = 10.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3149, 9906, 0),
                            dest = CoordGrid(3155, 9906, 0),
                            level = 51,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3150, 9905, 0),
                            dest = CoordGrid(3155, 9906, 0),
                            level = 51,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3150, 9907, 0),
                            dest = CoordGrid(3155, 9906, 0),
                            level = 51,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3154, 9905, 0),
                            dest = CoordGrid(3149, 9906, 0),
                            level = 51,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3154, 9907, 0),
                            dest = CoordGrid(3149, 9906, 0),
                            level = 51,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3155, 9906, 0),
                            dest = CoordGrid(3149, 9906, 0),
                            level = 51,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_archeuus_runestone_shortcut_midgrey_bottom_climb",
                loc = "loc.archeuus_runestone_shortcut_midgrey_bottom",
                option = "Climb",
                level = 52,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1769, 3849, 0),
                            dest = CoordGrid(1774, 3849, 0),
                            level = 52,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1774, 3849, 0),
                            dest = CoordGrid(1769, 3849, 0),
                            level = 52,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_xbows_jungletree_karamja_basic_grapple",
                loc = "loc.xbows_jungletree_karamja_basic",
                option = "Grapple",
                level = 53,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2874, 3127, 0),
                            dest = CoordGrid(2874, 3133, 0),
                            level = 53,
                            ranged = 42,
                            strength = 21,
                            gear = GEAR_GRAPPLE,
                            bareLevel = 78,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2874, 3133, 0),
                            dest = CoordGrid(2874, 3127, 0),
                            level = 53,
                            ranged = 42,
                            strength = 21,
                            gear = GEAR_GRAPPLE,
                            bareLevel = 78,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2874, 3136, 0),
                            dest = CoordGrid(2874, 3142, 0),
                            level = 53,
                            ranged = 42,
                            strength = 21,
                            gear = GEAR_GRAPPLE,
                            bareLevel = 78,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2874, 3142, 0),
                            dest = CoordGrid(2874, 3136, 0),
                            level = 53,
                            ranged = 42,
                            strength = 21,
                            gear = GEAR_GRAPPLE,
                            bareLevel = 78,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_aldarin_cliff_shortcut_bottom_climb",
                loc = "loc.aldarin_cliff_shortcut_bottom",
                option = "Climb",
                level = 54,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1338, 2917, 0),
                            dest = CoordGrid(1343, 2917, 0),
                            level = 54,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1343, 2917, 0),
                            dest = CoordGrid(1338, 2917, 0),
                            level = 54,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_misc_diary_steppingstone_cross",
                loc = "loc.misc_diary_steppingstone",
                option = "Cross",
                level = 55,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2573, 3859, 0),
                            dest = CoordGrid(2575, 3861, 0),
                            level = 55,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2575, 3861, 0),
                            dest = CoordGrid(2573, 3859, 0),
                            level = 55,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_karamja_dungeon_stepping_stone_end_cross",
                loc = "loc.karamja_dungeon_stepping_stone_end",
                option = "Cross",
                level = 56,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2682, 9548, 0),
                            dest = CoordGrid(2690, 9547, 0),
                            level = 56,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2697, 9525, 0),
                            dest = CoordGrid(2695, 9533, 0),
                            level = 56,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_viking_pike_defence_broken_jump",
                loc = "loc.viking_pike_defence_broken",
                option = "Jump",
                level = 57,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2688, 3697, 0),
                            dest = CoordGrid(2691, 3697, 0),
                            level = 57,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2691, 3697, 0),
                            dest = CoordGrid(2688, 3697, 0),
                            level = 57,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_elves_overpass_sc_rocks_bottom_climb",
                loc = "loc.elves_overpass_sc_rocks_bottom",
                option = "Climb",
                level = 59,
                xp = 0.0,
                ticks = 6,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2346, 3300, 0),
                            dest = CoordGrid(2344, 3294, 0),
                            level = 59,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2338, 3286, 0),
                            dest = CoordGrid(2338, 3281, 0),
                            level = 68,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2338, 3253, 0),
                            dest = CoordGrid(2332, 3252, 0),
                            level = 85,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_elves_overpass_sc_rocks_top_climb",
                loc = "loc.elves_overpass_sc_rocks_top",
                option = "Climb",
                level = 59,
                xp = 0.0,
                ticks = 8,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2344, 3294, 0),
                            dest = CoordGrid(2346, 3300, 0),
                            level = 59,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2338, 3281, 0),
                            dest = CoordGrid(2338, 3286, 0),
                            level = 68,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2332, 3252, 0),
                            dest = CoordGrid(2338, 3253, 0),
                            level = 85,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_godwars_climbing_rocks_down_climb",
                loc = "loc.godwars_climbing_rocks_down",
                option = "Climb",
                level = 60,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2927, 3761, 0),
                            dest = CoordGrid(2928, 3757, 0),
                            level = 60,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_godwars_climbing_rocks_up_climb",
                loc = "loc.godwars_climbing_rocks_up",
                option = "Climb",
                level = 60,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2927, 3758, 0),
                            dest = CoordGrid(2927, 3761, 0),
                            level = 60,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2928, 3757, 0),
                            dest = CoordGrid(2927, 3761, 0),
                            level = 60,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2929, 3758, 0),
                            dest = CoordGrid(2927, 3761, 0),
                            level = 60,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_mos_les_stepping_stone_jump_to",
                loc = "loc.mos_les_stepping_stone",
                option = "Jump-to",
                level = 60,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3708, 2969, 0),
                            dest = CoordGrid(3714, 2969, 0),
                            level = 60,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3714, 2969, 0),
                            dest = CoordGrid(3708, 2969, 0),
                            level = 60,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_slayer_dungeon_2_sc_wall_crack_squeeze_through",
                loc = "loc.slayer_dungeon_2_sc_wall_crack",
                option = "Squeeze-through",
                level = 61,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2730, 10008, 0),
                            dest = CoordGrid(2735, 10008, 0),
                            level = 61,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2735, 10008, 0),
                            dest = CoordGrid(2730, 10008, 0),
                            level = 61,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_slayertower_sc_chainbottom_climb_up",
                loc = "loc.slayertower_sc_chainbottom",
                option = "Climb-up",
                level = 61,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3421, 3550, 0),
                            dest = CoordGrid(3421, 3550, 1),
                            level = 61,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3422, 3549, 0),
                            dest = CoordGrid(3422, 3549, 1),
                            level = 61,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3422, 3551, 0),
                            dest = CoordGrid(3422, 3551, 1),
                            level = 61,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3423, 3550, 0),
                            dest = CoordGrid(3423, 3550, 1),
                            level = 61,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3446, 3576, 1),
                            dest = CoordGrid(3446, 3576, 2),
                            level = 71,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3447, 3575, 1),
                            dest = CoordGrid(3447, 3575, 2),
                            level = 71,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3447, 3577, 1),
                            dest = CoordGrid(3447, 3577, 2),
                            level = 71,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3448, 3576, 1),
                            dest = CoordGrid(3448, 3576, 2),
                            level = 71,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_slayertower_sc_chaintop_climb_down",
                loc = "loc.slayertower_sc_chaintop",
                option = "Climb-down",
                level = 61,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3421, 3550, 1),
                            dest = CoordGrid(3421, 3550, 0),
                            level = 61,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3422, 3549, 1),
                            dest = CoordGrid(3422, 3549, 0),
                            level = 61,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3422, 3551, 1),
                            dest = CoordGrid(3422, 3551, 0),
                            level = 61,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3423, 3550, 1),
                            dest = CoordGrid(3423, 3550, 0),
                            level = 61,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3446, 3576, 2),
                            dest = CoordGrid(3446, 3576, 1),
                            level = 71,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3447, 3575, 2),
                            dest = CoordGrid(3447, 3575, 1),
                            level = 71,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3447, 3577, 2),
                            dest = CoordGrid(3447, 3577, 1),
                            level = 71,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3448, 3576, 2),
                            dest = CoordGrid(3448, 3576, 1),
                            level = 71,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_mount_karuulm_shortcut_rocks_climb",
                loc = "loc.mount_karuulm_shortcut_rocks",
                option = "Climb",
                level = 62,
                xp = 0.0,
                ticks = 8,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1324, 3787, 0),
                            dest = CoordGrid(1324, 3795, 0),
                            level = 62,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1324, 3795, 0),
                            dest = CoordGrid(1324, 3787, 0),
                            level = 62,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_necropolis_stepping_stone_1_cross",
                loc = "loc.necropolis_stepping_stone_1",
                option = "Cross",
                level = 62,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3289, 2700, 0),
                            dest = CoordGrid(3295, 2700, 0),
                            level = 62,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3295, 2700, 0),
                            dest = CoordGrid(3289, 2700, 0),
                            level = 62,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_necropolis_stepping_stone_2_cross",
                loc = "loc.necropolis_stepping_stone_2",
                option = "Cross",
                level = 62,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3295, 2704, 0),
                            dest = CoordGrid(3295, 2708, 0),
                            level = 62,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3295, 2708, 0),
                            dest = CoordGrid(3295, 2704, 0),
                            level = 62,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_darkm_outer_wall_2h_shortcut_climb",
                loc = "loc.darkm_outer_wall_2h_shortcut",
                option = "Climb",
                level = 63,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3667, 3375, 0),
                            dest = CoordGrid(3670, 3375, 0),
                            level = 63,
                            varSymbol = "varbit.darkm_shortcut_inner",
                            varValue = 1,
                            varExact = true,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3670, 3375, 0),
                            dest = CoordGrid(3667, 3375, 0),
                            level = 63,
                            varSymbol = "varbit.darkm_shortcut_inner",
                            varValue = 1,
                            varExact = true,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_darkm_outer_wall_3h_shortcut_climb",
                loc = "loc.darkm_outer_wall_3h_shortcut",
                option = "Climb",
                level = 63,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3670, 3375, 0),
                            dest = CoordGrid(3673, 3375, 0),
                            level = 63,
                            varSymbol = "varbit.darkm_shortcut_outer",
                            varValue = 1,
                            varExact = true,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3672, 3374, 0),
                            dest = CoordGrid(3670, 3375, 0),
                            level = 63,
                            varSymbol = "varbit.darkm_shortcut_outer",
                            varValue = 1,
                            varExact = true,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3672, 3376, 0),
                            dest = CoordGrid(3670, 3375, 0),
                            level = 63,
                            varSymbol = "varbit.darkm_shortcut_outer",
                            varValue = 1,
                            varExact = true,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3673, 3375, 0),
                            dest = CoordGrid(3670, 3375, 0),
                            level = 63,
                            varSymbol = "varbit.darkm_shortcut_outer",
                            varValue = 1,
                            varExact = true,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_deepdungeonlooserailing_squeeze_through",
                loc = "loc.deepdungeonlooserailing",
                option = "Squeeze-through",
                level = 63,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2935, 9810, 0),
                            dest = CoordGrid(2936, 9810, 0),
                            level = 63,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2936, 9810, 0),
                            dest = CoordGrid(2935, 9810, 0),
                            level = 63,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_hosdun_agility_shortcut_jump_over",
                loc = "loc.hosdun_agility_shortcut",
                option = "Jump-over",
                level = 63,
                xp = 10.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1820, 9945, 0),
                            dest = CoordGrid(1820, 9947, 0),
                            level = 63,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1820, 9947, 0),
                            dest = CoordGrid(1820, 9945, 0),
                            level = 63,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_priestperil_tomb_cornerl_jump_over",
                loc = "loc.priestperil_tomb_cornerl",
                option = "Jump-over",
                level = 63,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1820, 9944, 0),
                            dest = CoordGrid(1820, 9946, 0),
                            level = 63,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1820, 9946, 0),
                            dest = CoordGrid(1820, 9944, 0),
                            level = 63,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_fossil_volcano_agility_rope_bottom_climb",
                loc = "loc.fossil_volcano_agility_rope_bottom",
                option = "Climb",
                level = 64,
                xp = 0.0,
                ticks = 6,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3778, 3821, 0),
                            dest = CoordGrid(3784, 3821, 0),
                            level = 64,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_fossil_volcano_agility_rope_top_climb",
                loc = "loc.fossil_volcano_agility_rope_top",
                option = "Climb",
                level = 64,
                xp = 0.0,
                ticks = 6,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3784, 3821, 0),
                            dest = CoordGrid(3778, 3821, 0),
                            level = 64,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_trollheim_wildy_climb_rocks_climb",
                loc = "loc.trollheim_wildy_climb_rocks",
                option = "Climb",
                level = 64,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2915, 3672, 0),
                            dest = CoordGrid(2918, 3672, 0),
                            level = 64,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2918, 3672, 0),
                            dest = CoordGrid(2915, 3672, 0),
                            level = 64,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2921, 3672, 0),
                            dest = CoordGrid(2924, 3673, 0),
                            level = 64,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2924, 3673, 0),
                            dest = CoordGrid(2921, 3672, 0),
                            level = 64,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2946, 3678, 0),
                            dest = CoordGrid(2949, 3681, 0),
                            level = 64,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_morytania_climbingrocks_sc_bottom_climb",
                loc = "loc.morytania_climbingrocks_sc_bottom",
                option = "Climb",
                level = 65,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3426, 3478, 0),
                            dest = CoordGrid(3424, 3476, 0),
                            level = 65,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3427, 3477, 0),
                            dest = CoordGrid(3424, 3476, 0),
                            level = 65,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_morytania_climbingrocks_sc_top_climb",
                loc = "loc.morytania_climbingrocks_sc_top",
                option = "Climb",
                level = 65,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3424, 3476, 0),
                            dest = CoordGrid(3427, 3477, 0),
                            level = 65,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_morytania_railing_sc_fence_1_squeeze_through",
                loc = "loc.morytania_railing_sc_fence_1",
                option = "Squeeze-through",
                level = 65,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3423, 3476, 0),
                            dest = CoordGrid(3424, 3476, 0),
                            level = 65,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3424, 3476, 0),
                            dest = CoordGrid(3423, 3476, 0),
                            level = 65,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_morytania_railing_sc_fence_2_squeeze_through",
                loc = "loc.morytania_railing_sc_fence_2",
                option = "Squeeze-through",
                level = 65,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3425, 3483, 0),
                            dest = CoordGrid(3425, 3484, 0),
                            level = 65,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3425, 3484, 0),
                            dest = CoordGrid(3425, 3483, 0),
                            level = 65,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_lumbridge_diary_desert_shortcut_jump_to",
                loc = "loc.lumbridge_diary_desert_shortcut",
                option = "Jump-to",
                level = 66,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3212, 3137, 0),
                            dest = CoordGrid(3214, 3132, 0),
                            level = 66,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3212, 3138, 0),
                            dest = CoordGrid(3214, 3131, 0),
                            level = 66,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3214, 3131, 0),
                            dest = CoordGrid(3212, 3138, 0),
                            level = 66,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3214, 3132, 0),
                            dest = CoordGrid(3212, 3137, 0),
                            level = 66,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_tavelryshortcut_climb_up",
                loc = "loc.tavelryshortcut",
                option = "Climb-up",
                level = 66,
                xp = 25.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2946, 3439, 0),
                            dest = CoordGrid(2943, 3439, 0),
                            level = 66,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_climbingcaverocks1_climb_up",
                loc = "loc.climbingcaverocks1",
                option = "Climb-up",
                level = 67,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2616, 9571, 0),
                            dest = CoordGrid(2615, 9506, 0),
                            level = 67,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2617, 9571, 0),
                            dest = CoordGrid(2615, 9506, 0),
                            level = 67,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_climbingcaverocks2_climb_down",
                loc = "loc.climbingcaverocks2",
                option = "Climb-down",
                level = 67,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2614, 9504, 0),
                            dest = CoordGrid(2616, 9571, 0),
                            level = 67,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2614, 9505, 0),
                            dest = CoordGrid(2616, 9571, 0),
                            level = 67,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2615, 9503, 0),
                            dest = CoordGrid(2616, 9571, 0),
                            level = 67,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2615, 9506, 0),
                            dest = CoordGrid(2616, 9571, 0),
                            level = 67,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2616, 9503, 0),
                            dest = CoordGrid(2616, 9571, 0),
                            level = 67,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2616, 9506, 0),
                            dest = CoordGrid(2616, 9571, 0),
                            level = 67,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_ice_mountain_shortcut_bottom_climb",
                loc = "loc.ice_mountain_shortcut_bottom",
                option = "Climb",
                level = 68,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2997, 3483, 0),
                            dest = CoordGrid(3002, 3483, 0),
                            level = 68,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_ice_mountain_shortcut_top_climb",
                loc = "loc.ice_mountain_shortcut_top",
                option = "Climb",
                level = 68,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3002, 3483, 0),
                            dest = CoordGrid(2997, 3483, 0),
                            level = 68,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_archeuus_runestone_shortcut_grey_shortcut_north_climb",
                loc = "loc.archeuus_runestone_shortcut_grey_shortcut_north",
                option = "Climb",
                level = 69,
                xp = 0.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1761, 3872, 0),
                            dest = CoordGrid(1761, 3874, 0),
                            level = 69,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1761, 3874, 0),
                            dest = CoordGrid(1761, 3872, 0),
                            level = 69,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_darkm_wall_rock_shortcut_jump_over",
                loc = "loc.darkm_wall_rock_shortcut",
                option = "Jump-over",
                level = 69,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3565, 3379, 0),
                            dest = CoordGrid(3562, 3379, 0),
                            level = 69,
                            quest = "quest_sinsofthefather",
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_fenk_bridge_multi_north_cross",
                loc = "loc.fenk_bridge_multi_north",
                option = "Cross",
                level = 69,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3503, 3562, 0),
                            dest = CoordGrid(3503, 3559, 0),
                            level = 69,
                            varSymbol = "varbit.fenk_built_bridge_south",
                            varValue = 2,
                            varExact = true,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_fenk_bridge_multi_north_mirror_cross",
                loc = "loc.fenk_bridge_multi_north_mirror",
                option = "Cross",
                level = 69,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3504, 3562, 0),
                            dest = CoordGrid(3504, 3559, 0),
                            level = 69,
                            varSymbol = "varbit.fenk_built_bridge_south",
                            varValue = 2,
                            varExact = true,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_fenk_bridge_multi_south_cross",
                loc = "loc.fenk_bridge_multi_south",
                option = "Cross",
                level = 69,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3504, 3559, 0),
                            dest = CoordGrid(3504, 3562, 0),
                            level = 69,
                            varSymbol = "varbit.fenk_built_bridge_south",
                            varValue = 2,
                            varExact = true,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_fenk_bridge_multi_south_mirror_cross",
                loc = "loc.fenk_bridge_multi_south_mirror",
                option = "Cross",
                level = 69,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3503, 3559, 0),
                            dest = CoordGrid(3503, 3562, 0),
                            level = 69,
                            varSymbol = "varbit.fenk_built_bridge_south",
                            varValue = 2,
                            varExact = true,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_hh_master005_jump_over",
                loc = "loc.hh_master005",
                option = "Jump-over",
                level = 69,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3562, 3379, 0),
                            dest = CoordGrid(3565, 3379, 0),
                            level = 69,
                            quest = "quest_sinsofthefather",
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_fossil_shortcut_basecamp_a_climb",
                loc = "loc.fossil_shortcut_basecamp_a",
                option = "Climb",
                level = 70,
                xp = 0.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3713, 3830, 0),
                            dest = CoordGrid(3715, 3815, 0),
                            level = 70,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3714, 3830, 0),
                            dest = CoordGrid(3715, 3815, 0),
                            level = 70,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3715, 3830, 0),
                            dest = CoordGrid(3715, 3815, 0),
                            level = 70,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_fossil_shortcut_basecamp_b_climb",
                loc = "loc.fossil_shortcut_basecamp_b",
                option = "Climb",
                level = 70,
                xp = 0.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3713, 3816, 0),
                            dest = CoordGrid(3713, 3830, 0),
                            level = 70,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3714, 3815, 0),
                            dest = CoordGrid(3713, 3830, 0),
                            level = 70,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3715, 3815, 0),
                            dest = CoordGrid(3713, 3830, 0),
                            level = 70,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3716, 3815, 0),
                            dest = CoordGrid(3713, 3830, 0),
                            level = 70,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3717, 3815, 0),
                            dest = CoordGrid(3713, 3830, 0),
                            level = 70,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3718, 3816, 0),
                            dest = CoordGrid(3713, 3830, 0),
                            level = 70,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_kharid_poshwall_topless_climb",
                loc = "loc.kharid_poshwall_topless",
                option = "Climb",
                level = 70,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3295, 3157, 0),
                            dest = CoordGrid(3295, 3159, 0),
                            level = 70,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_taverly_dungeon_pipe_sc_squeeze_through",
                loc = "loc.taverly_dungeon_pipe_sc",
                option = "Squeeze-through",
                level = 70,
                xp = 10.0,
                ticks = 3,
                fail = FailDecl(0, 254, 0.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2886, 9799, 0),
                            dest = CoordGrid(2892, 9799, 0),
                            level = 70,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2892, 9799, 0),
                            dest = CoordGrid(2886, 9799, 0),
                            level = 70,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_polli_stepping_stone_cross",
                loc = "loc.polli_stepping_stone",
                option = "Cross",
                level = 71,
                xp = 25.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3372, 2959, 0),
                            dest = CoordGrid(3373, 2955, 0),
                            level = 71,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3373, 2955, 0),
                            dest = CoordGrid(3372, 2959, 0),
                            level = 71,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_proudspire_climbing_rocks_grey03_op_climb",
                loc = "loc.proudspire_climbing_rocks_grey03_op",
                option = "Climb",
                level = 71,
                xp = 0.0,
                ticks = 8,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1576, 3254, 0),
                            dest = CoordGrid(1581, 3249, 0),
                            level = 71,
                        ),
                        LinkDecl(
                            origin = CoordGrid(1581, 3249, 0),
                            dest = CoordGrid(1576, 3254, 0),
                            level = 71,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_wilderness_chaos_temple_shortcut_cross",
                loc = "loc.wilderness_chaos_temple_shortcut",
                option = "Cross",
                level = 72,
                xp = 0.0,
                ticks = 2,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3268, 3625, 0),
                            dest = CoordGrid(3268, 3629, 0),
                            level = 72,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3268, 3629, 0),
                            dest = CoordGrid(3268, 3625, 0),
                            level = 72,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_archeuus_runestone_shortcut_grey_top_climb",
                loc = "loc.archeuus_runestone_shortcut_grey_top",
                option = "Climb",
                level = 73,
                xp = 0.0,
                ticks = 4,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(1742, 3854, 0),
                            dest = CoordGrid(1752, 3854, 0),
                            level = 73,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_diary_troll_climbingrocks_climb",
                loc = "loc.diary_troll_climbingrocks",
                option = "Climb",
                level = 73,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2838, 3693, 0),
                            dest = CoordGrid(2844, 3693, 0),
                            level = 73,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2843, 3692, 0),
                            dest = CoordGrid(2838, 3693, 0),
                            level = 73,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2844, 3693, 0),
                            dest = CoordGrid(2838, 3693, 0),
                            level = 73,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_wilderness_lava_dragons_shortcut_cross",
                loc = "loc.wilderness_lava_dragons_shortcut",
                option = "Cross",
                level = 74,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3201, 3807, 0),
                            dest = CoordGrid(3201, 3810, 0),
                            level = 74,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3201, 3810, 0),
                            dest = CoordGrid(3201, 3807, 0),
                            level = 74,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_snakeboss_steppingstone_cross",
                loc = "loc.snakeboss_steppingstone",
                option = "Cross",
                level = 76,
                xp = 0.0,
                ticks = 6,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2154, 3072, 0),
                            dest = CoordGrid(2160, 3072, 0),
                            level = 76,
                            quest = "quest_regicide",
                        ),
                        LinkDecl(
                            origin = CoordGrid(2160, 3072, 0),
                            dest = CoordGrid(2154, 3072, 0),
                            level = 76,
                            quest = "quest_regicide",
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_kharazi_shortcut_vine_diag1_live_climb",
                loc = "loc.kharazi_shortcut_vine_diag1_live",
                option = "Climb",
                level = 79,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2899, 2937, 0),
                            dest = CoordGrid(2899, 2942, 0),
                            level = 79,
                            quest = "quest_legends",
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_kharazi_shortcut_vine_end_live_climb",
                loc = "loc.kharazi_shortcut_vine_end_live",
                option = "Climb",
                level = 79,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2899, 2942, 0),
                            dest = CoordGrid(2899, 2937, 0),
                            level = 79,
                            quest = "quest_legends",
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_shortcut_shilo_rocks_bottom_climb",
                loc = "loc.shortcut_shilo_rocks_bottom",
                option = "Climb",
                level = 79,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2871, 3008, 0),
                            dest = CoordGrid(2871, 3003, 0),
                            level = 79,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_shortcut_shilo_rocks_top_climb",
                loc = "loc.shortcut_shilo_rocks_top",
                option = "Climb",
                level = 79,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2871, 3003, 0),
                            dest = CoordGrid(2871, 3008, 0),
                            level = 79,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_taverly_dungeon_floor_spikes_sc_jump_over",
                loc = "loc.taverly_dungeon_floor_spikes_sc",
                option = "Jump-over",
                level = 80,
                xp = 12.5,
                ticks = 3,
                fail = FailDecl(0, 220, 0.0, null),
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2878, 9813, 0),
                            dest = CoordGrid(2881, 9813, 0),
                            level = 80,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2880, 9813, 0),
                            dest = CoordGrid(2877, 9813, 0),
                            level = 80,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_slayer_corpse1_climb_up",
                loc = "loc.slayer_corpse1",
                option = "Climb-up",
                level = 81,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3418, 3532, 0),
                            dest = CoordGrid(3420, 3534, 2),
                            level = 81,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_wilderness_lava_maze_northern_shortcut_cross",
                loc = "loc.wilderness_lava_maze_northern_shortcut",
                option = "Cross",
                level = 82,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3092, 3878, 0),
                            dest = CoordGrid(3092, 3883, 0),
                            level = 82,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3092, 3883, 0),
                            dest = CoordGrid(3092, 3878, 0),
                            level = 82,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_crandor_shortcut_bottom_climb",
                loc = "loc.crandor_shortcut_bottom",
                option = "Climb",
                level = 84,
                xp = 0.0,
                ticks = 6,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2832, 3247, 0),
                            dest = CoordGrid(2832, 3252, 0),
                            level = 84,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_crandor_shortcut_top_climb",
                loc = "loc.crandor_shortcut_top",
                option = "Climb",
                level = 84,
                xp = 0.0,
                ticks = 6,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2832, 3252, 0),
                            dest = CoordGrid(2832, 3247, 0),
                            level = 84,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_dagannoth_waterbirth_rock_climb_agility_shortcut_bottom_climb",
                loc = "loc.dagannoth_waterbirth_rock_climb_agility_shortcut_bottom",
                option = "Climb",
                level = 85,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2547, 3749, 0),
                            dest = CoordGrid(2547, 3744, 0),
                            level = 85,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_dagannoth_waterbirth_rock_climb_agility_shortcut_top_climb",
                loc = "loc.dagannoth_waterbirth_rock_climb_agility_shortcut_top",
                option = "Climb",
                level = 85,
                xp = 0.0,
                ticks = 5,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2547, 3744, 0),
                            dest = CoordGrid(2547, 3749, 0),
                            level = 85,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_kalphite_wall_shortcut_squeeze_through",
                loc = "loc.kalphite_wall_shortcut",
                option = "Squeeze-through",
                level = 86,
                xp = 0.0,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(3500, 9510, 2),
                            dest = CoordGrid(3506, 9505, 2),
                            level = 86,
                        ),
                        LinkDecl(
                            origin = CoordGrid(3506, 9505, 2),
                            dest = CoordGrid(3500, 9510, 2),
                            level = 86,
                        ),
                    ),
            ),
            ShortcutDecl(
                row = "agility_sc_legends_quest_cave_shortcut_squeeze_through",
                loc = "loc.legends_quest_cave_shortcut",
                option = "Squeeze-through",
                level = 96,
                xp = 7.5,
                ticks = 3,
                links =
                    listOf(
                        LinkDecl(
                            origin = CoordGrid(2764, 9341, 0),
                            dest = CoordGrid(2769, 9340, 0),
                            level = 96,
                        ),
                        LinkDecl(
                            origin = CoordGrid(2769, 9340, 0),
                            dest = CoordGrid(2764, 9341, 0),
                            level = 96,
                        ),
                    ),
            ),
        )
}

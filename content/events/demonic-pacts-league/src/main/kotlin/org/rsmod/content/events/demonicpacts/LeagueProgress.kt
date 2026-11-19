package org.rsmod.content.events.demonicpacts

import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType

private val PACT_VARPS =
    listOf(
        "varp.dp_pacts_0",
        "varp.dp_pacts_1",
        "varp.dp_pacts_2",
        "varp.dp_pacts_3",
        "varp.dp_pacts_4",
    )

private val TASK_VARPS =
    listOf(
        "varp.dp_tasks_0",
        "varp.dp_tasks_1",
        "varp.dp_tasks_2",
        "varp.dp_tasks_3",
        "varp.dp_tasks_4",
        "varp.dp_tasks_5",
        "varp.dp_tasks_6",
        "varp.dp_tasks_7",
    )

private const val INITIALISED = "varp.dp_initialised"
private const val LEAGUE_POINTS = "varp.dp_league_points"
private const val TASKS_DONE = "varp.dp_tasks_done"
private const val PACT_POINTS_EARNED = "varp.dp_pact_points_earned"
private const val PACT_RESETS_USED = "varp.dp_pact_resets_used"
private const val REGIONS = "varp.dp_regions"
private const val TOTAL_KILLS = "varp.dp_total_kills"

val LEAGUE_STATS: List<String> =
    listOf(
        "stat.attack",
        "stat.defence",
        "stat.strength",
        "stat.hitpoints",
        "stat.ranged",
        "stat.prayer",
        "stat.magic",
        "stat.cooking",
        "stat.woodcutting",
        "stat.fletching",
        "stat.fishing",
        "stat.firemaking",
        "stat.crafting",
        "stat.smithing",
        "stat.mining",
        "stat.herblore",
        "stat.agility",
        "stat.thieving",
        "stat.slayer",
        "stat.farming",
        "stat.runecrafting",
        "stat.hunter",
        "stat.construction",
    )

val COMBAT_STATS: Set<String> =
    setOf(
        "stat.attack",
        "stat.defence",
        "stat.strength",
        "stat.hitpoints",
        "stat.ranged",
        "stat.prayer",
        "stat.magic",
    )

val Player.isDemonicPactsLeague: Boolean
    get() = worldType == WorldType.DEMONIC_PACTS_LEAGUE

var Player.dpInitialised: Boolean
    get() = vars[INITIALISED] == 1
    set(value) = VarPlayerIntMapSetter.set(this, INITIALISED, if (value) 1 else 0)

var Player.leaguePoints: Int
    get() = vars[LEAGUE_POINTS]
    set(value) = VarPlayerIntMapSetter.set(this, LEAGUE_POINTS, value)

var Player.leagueTasksDone: Int
    get() = vars[TASKS_DONE]
    set(value) = VarPlayerIntMapSetter.set(this, TASKS_DONE, value)

var Player.pactPointsEarned: Int
    get() = vars[PACT_POINTS_EARNED]
    set(value) = VarPlayerIntMapSetter.set(this, PACT_POINTS_EARNED, value)

var Player.pactResetsUsed: Int
    get() = vars[PACT_RESETS_USED]
    set(value) = VarPlayerIntMapSetter.set(this, PACT_RESETS_USED, value)

var Player.leagueTotalKills: Int
    get() = vars[TOTAL_KILLS]
    set(value) = VarPlayerIntMapSetter.set(this, TOTAL_KILLS, value)

private var Player.regionBits: Int
    get() = vars[REGIONS]
    set(value) = VarPlayerIntMapSetter.set(this, REGIONS, value)

fun Player.hasRegion(region: LeagueRegion): Boolean =
    region.bit < 0 || (regionBits ushr region.bit) and 1 == 1

fun Player.unlockRegion(region: LeagueRegion) {
    if (region.bit >= 0) {
        regionBits = regionBits or (1 shl region.bit)
    }
}

fun Player.unlockedRegions(): List<LeagueRegion> =
    DemonicPactsData.regions.filter { it.bit >= 0 && hasRegion(it) }

fun Player.chosenRegionCount(): Int =
    DemonicPactsData.regions.count { it.unlockable && hasRegion(it) }

fun Player.ownsPact(node: PactNode): Boolean = getBit(PACT_VARPS, node.bit)

fun Player.setPactOwned(node: PactNode, owned: Boolean) = setBit(PACT_VARPS, node.bit, owned)

fun Player.ownedPacts(): List<PactNode> = DemonicPactsData.nodes.filter { ownsPact(it) }

fun Player.clearAllPacts() {
    for (varp in PACT_VARPS) {
        VarPlayerIntMapSetter.set(this, varp, 0)
    }
}

fun Player.isTaskDone(task: LeagueTask): Boolean = getBit(TASK_VARPS, task.bit)

fun Player.setTaskDone(task: LeagueTask) = setBit(TASK_VARPS, task.bit, true)

fun Player.relicTier(): Int {
    val points = leaguePoints
    return DemonicPactsData.settings.tierThresholds.count { points >= it }
}

fun Player.pactPointsAvailable(): Int {
    val settings = DemonicPactsData.settings
    val owned = ownedPacts().size
    val spendable = minOf(pactPointsEarned, settings.maxPactsSpent)
    return (spendable - owned).coerceAtLeast(0)
}

fun Player.leagueTotalLevel(): Int = LEAGUE_STATS.sumOf { statBase(it) }

private fun Player.getBit(varps: List<String>, bit: Int): Boolean {
    val value = vars[varps[bit / 32]]
    return (value ushr (bit % 32)) and 1 == 1
}

private fun Player.setBit(varps: List<String>, bit: Int, on: Boolean) {
    val varp = varps[bit / 32]
    val mask = 1 shl (bit % 32)
    val value = vars[varp]
    VarPlayerIntMapSetter.set(this, varp, if (on) value or mask else value and mask.inv())
}

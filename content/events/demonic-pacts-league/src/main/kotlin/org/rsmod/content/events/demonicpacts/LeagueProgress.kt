package org.rsmod.content.events.demonicpacts

import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType

private val PACT_VARPS =
    listOf(
        "varp.combat_mastery_perm_0",
        "varp.combat_mastery_perm_1",
        "varp.combat_mastery_perm_2",
        "varp.combat_mastery_perm_3",
        "varp.combat_mastery_perm_4",
        "varp.combat_mastery_perm_5",
    )

private val LEGACY_PACT_VARPS =
    listOf(
        "varp.dp_pacts_0",
        "varp.dp_pacts_1",
        "varp.dp_pacts_2",
        "varp.dp_pacts_3",
        "varp.dp_pacts_4",
    )

private val LEGACY_TASK_VARPS =
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

/** The client's own task completion bits, one per entry of the league task list. */
private val TASK_VARPS =
    listOf(
        "varp.league_task_completed_0",
        "varp.league_task_completed_1",
        "varp.league_task_completed_2",
        "varp.league_task_completed_3",
        "varp.league_task_completed_4",
        "varp.league_task_completed_5",
        "varp.league_task_completed_6",
        "varp.league_task_completed_7",
        "varp.league_task_completed_8",
        "varp.league_task_completed_9",
        "varp.league_task_completed_10",
        "varp.league_task_completed_11",
        "varp.league_task_completed_12",
        "varp.league_task_completed_13",
        "varp.league_task_completed_14",
        "varp.league_task_completed_15",
        "varp.league_task_completed_16",
        "varp.league_task_completed_17",
        "varp.league_task_completed_18",
        "varp.league_task_completed_19",
        "varp.league_task_completed_20",
        "varp.league_task_completed_21",
        "varp.league_task_completed_22",
        "varp.league_task_completed_23",
        "varp.league_task_completed_24",
        "varp.league_task_completed_25",
        "varp.league_task_completed_26",
        "varp.league_task_completed_27",
        "varp.league_task_completed_28",
        "varp.league_task_completed_29",
        "varp.league_task_completed_30",
        "varp.league_task_completed_31",
        "varp.league_task_completed_32",
        "varp.league_task_completed_33",
        "varp.league_task_completed_34",
        "varp.league_task_completed_35",
        "varp.league_task_completed_36",
        "varp.league_task_completed_37",
        "varp.league_task_completed_38",
        "varp.league_task_completed_39",
        "varp.league_task_completed_40",
        "varp.league_task_completed_41",
        "varp.league_task_completed_42",
        "varp.league_task_completed_43",
        "varp.league_task_completed_44",
        "varp.league_task_completed_45",
        "varp.league_task_completed_46",
        "varp.league_task_completed_47",
        "varp.league_task_completed_48",
        "varp.league_task_completed_49",
        "varp.league_task_completed_50",
        "varp.league_task_completed_51",
        "varp.league_task_completed_52",
        "varp.league_task_completed_53",
        "varp.league_task_completed_54",
        "varp.league_task_completed_55",
        "varp.league_task_completed_56",
        "varp.league_task_completed_57",
        "varp.league_task_completed_58",
        "varp.league_task_completed_59",
        "varp.league_task_completed_60",
        "varp.league_task_completed_61",
    )

private const val INITIALISED = "varp.dp_initialised"
private const val LEAGUE_POINTS = "varp.dp_league_points"
private const val TASKS_DONE = "varp.dp_tasks_done"
private const val PACT_POINTS_EARNED = "varp.dp_pact_points_earned"
private const val CLIENT_PACT_POINTS_EARNED = "varp.talent_points_earned"
private const val CLIENT_PACT_POINTS_SPENT = "varp.talent_points_spent"
private const val PACT_RESETS_AVAILABLE = "varbit.talent_resets_available"
private const val PACT_RESETS_MAX = 63
private const val LEAGUE_TYPE = "varbit.league_type"
private const val LEAGUE_TYPE_DEMONIC_PACTS = 6
private const val LEAGUE_TUTORIAL = "varbit.league_tutorial_completed"
private const val LEAGUE_TUTORIAL_DONE = 63
private const val CLIENT_LEAGUE_POINTS = "varp.league_points_claimed"
private const val CLIENT_LEAGUE_CURRENCY = "varp.league_points_currency"
private const val WORLD_FLAGS_VARP = "varp.map_flags_cached"

/** Members and seasonal (league) world flags; the client reads the league bit from here. */
const val LEAGUE_WORLD_FLAGS: Int = 0x1 or 0x40000000
private const val REGIONS = "varp.dp_regions"
private const val OFFICIAL_TASKS = "varp.dp_official_tasks"
private const val CLIENT_TASKS_COMPLETED = "varbit.league_total_tasks_completed"
private const val CLIENT_POINTS_COMPLETED = "varp.league_points_completed"
private val AREA_SELECTION_VARBITS =
    listOf(
        "varbit.league_area_selection_0",
        "varbit.league_area_selection_1",
        "varbit.league_area_selection_2",
        "varbit.league_area_selection_3",
        "varbit.league_area_selection_4",
    )
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

var Player.pactResetsAvailable: Int
    get() = vars[PACT_RESETS_AVAILABLE]
    set(value) =
        VarPlayerIntMapSetter.set(this, PACT_RESETS_AVAILABLE, value.coerceIn(0, PACT_RESETS_MAX))

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

fun Player.resetRegions() {
    regionBits = 0
    DemonicPactsData.regionsByKey["varlamore"]?.let { unlockRegion(it) }
    val karamja = DemonicPactsData.regionsByKey["karamja"]
    if (karamja != null && leagueTasksDone >= DemonicPactsData.settings.karamjaTasks) {
        unlockRegion(karamja)
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

/**
 * Pacts bought before the tree moved onto the cache's own node order were saved in different
 * slots. Clears them (their points come straight back, since spent points are counted from owned
 * pacts) and reports whether there was anything to clear.
 */
fun Player.clearLegacyPacts(): Boolean {
    var cleared = false
    for (varp in LEGACY_PACT_VARPS) {
        if (vars[varp] != 0) {
            VarPlayerIntMapSetter.set(this, varp, 0)
            cleared = true
        }
    }
    return cleared
}

/** Sends the league state the client's league interfaces read. */
fun Player.syncLeagueClientVars() {
    val settings = DemonicPactsData.settings
    setIfChanged(WORLD_FLAGS_VARP, LEAGUE_WORLD_FLAGS)
    setIfChanged(LEAGUE_TYPE, LEAGUE_TYPE_DEMONIC_PACTS)
    if (vars[LEAGUE_TUTORIAL] < LEAGUE_TUTORIAL_DONE) {
        setIfChanged(LEAGUE_TUTORIAL, LEAGUE_TUTORIAL_DONE)
    }
    setIfChanged(CLIENT_PACT_POINTS_EARNED, minOf(pactPointsEarned, settings.maxPactsSpent))
    setIfChanged(CLIENT_PACT_POINTS_SPENT, ownedPacts().size)
    setIfChanged(CLIENT_LEAGUE_POINTS, leaguePoints)
    setIfChanged(CLIENT_LEAGUE_CURRENCY, leaguePoints)
    setIfChanged(CLIENT_POINTS_COMPLETED, leaguePoints)
    setIfChanged(CLIENT_TASKS_COMPLETED, leagueTasksDone)
    syncAreaSelections()
}

private fun Player.setIfChanged(varName: String, value: Int) {
    if (vars[varName] != value) {
        VarPlayerIntMapSetter.set(this, varName, value)
    }
}

fun Player.isTaskDone(task: OfficialTask): Boolean = getBit(TASK_VARPS, task.index)

fun Player.setTaskDone(task: OfficialTask, done: Boolean = true) = setBit(TASK_VARPS, task.index, done)

/**
 * Moves a character from the old hand-written task list onto the official one, once: clears the
 * old task bits, task count and league points (relics and pacts stay).
 */
fun Player.migrateToOfficialTasks(): Boolean {
    if (vars[OFFICIAL_TASKS] == 1) {
        return false
    }
    for (varp in LEGACY_TASK_VARPS) {
        VarPlayerIntMapSetter.set(this, varp, 0)
    }
    for (varp in TASK_VARPS) {
        VarPlayerIntMapSetter.set(this, varp, 0)
    }
    leaguePoints = 0
    leagueTasksDone = 0
    VarPlayerIntMapSetter.set(this, OFFICIAL_TASKS, 1)
    return true
}

/** The area ids in the order the client's five League Areas slots hold them. */
fun Player.areaSelections(): List<Int> = AREA_SELECTION_VARBITS.map { vars[it] }

/**
 * Puts every unlocked region into the client's area slots: Varlamore first, Karamja second, then
 * the chosen regions in the order they were picked.
 */
fun Player.syncAreaSelections() {
    val unlocked = unlockedRegions().filter { it.area > 0 }
    val wanted = MutableList(AREA_SELECTION_VARBITS.size) { 0 }
    DemonicPactsData.regionsByKey["varlamore"]?.takeIf { hasRegion(it) }?.let { wanted[0] = it.area }
    DemonicPactsData.regionsByKey["karamja"]?.takeIf { hasRegion(it) }?.let { wanted[1] = it.area }
    val current = areaSelections()
    val chosen = unlocked.filter { it.unlockable }.map { it.area }
    val ordered = current.drop(2).filter { it in chosen } + chosen.filter { it !in current.drop(2) }
    ordered.take(AREA_SELECTION_VARBITS.size - 2).forEachIndexed { i, area -> wanted[i + 2] = area }
    AREA_SELECTION_VARBITS.forEachIndexed { i, varbit -> setIfChanged(varbit, wanted[i]) }
}

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

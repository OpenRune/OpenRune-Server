package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.reflect.KClass
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript

/** One placement per shortcut the tests cross, as the map has them. */
internal val SHORTCUT_PLACEMENTS =
    listOf(
        Placement("loc.troll_mountain_shortcut_climbingrocks1", CoordGrid(2885, 3683, 0), 0, 22),
        Placement("loc.ardougne_log_balance_right_sc", CoordGrid(2601, 3336, 0), 3, 22),
        Placement("loc.ardougne_log_balance_left_sc", CoordGrid(2599, 3336, 0), 3, 22),
        Placement("loc.zeah_cata_crack", CoordGrid(1648, 10008, 0), 1, 10),
        Placement("loc.agility_obstical_pipe_barbarian", CoordGrid(2552, 3559, 0), 1, 10),
        Placement("loc.troll_climbingrocks", CoordGrid(2821, 3635, 0), 2, 10),
        Placement("loc.heroes_guild_shortcut_to_fountain", CoordGrid(2899, 9901, 0), 0, 9),
        Placement("loc.gnome_stronghold_sc_rock_top", CoordGrid(2487, 3515, 0), 3, 22),
        Placement("loc.cavewall_shortcut_royal_titans_east", CoordGrid(2990, 9546, 0), 2, 10),
        Placement("loc.cavewall_shortcut_wyvern_west", CoordGrid(3014, 9549, 0), 0, 10),
        Placement("loc.xbows_jungletree_karamja_basic", CoordGrid(2873, 3134, 0), 3, 10),
    )

/** What one tick of a crossing left behind, read after the tick ran. */
internal class ShortcutTick(
    val coords: CoordGrid,
    val busy: Boolean,
    val xp: Int,
    val messages: List<String>,
    val mesbox: Boolean,
)

internal fun GameTestState.runShortcutTest(
    vararg scripts: KClass<out PluginScript>,
    body: GameTestScope.(AgilityTestDeps) -> Unit,
): Unit =
    runInjectedGameTest(
        AgilityTestDeps::class,
        AgilityModule(),
        AgilityCourseScript::class,
        *scripts,
    ) {
        deps ->
        advance()
        player.observerUUID = player.uuid
        for (placement in SHORTCUT_PLACEMENTS) {
            placeMapLoc(
                placement.coords,
                placement.loc,
                LocShape[placement.shape],
                LocAngle[placement.angle],
            )
        }
        body(deps)
    }

internal fun GameTestScope.shortcutLoc(loc: String, near: CoordGrid): BoundLocInfo {
    val placement =
        SHORTCUT_PLACEMENTS.filter { it.loc == loc }.minBy { it.coords.chebyshevDistance(near) }
    val type = checkNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)))
    val info =
        findLocs(placement.coords).first { it.coords == placement.coords && it.id == type.id }
    return BoundLocInfo(info, type)
}

/**
 * Clicks [loc] and records each tick until the crossing ends, from the tick the lane starts; the
 * ticks before it, spent reaching the start, are dropped.
 */
internal fun GameTestScope.crossShortcut(
    loc: String,
    near: CoordGrid = player.coords,
): List<ShortcutTick> {
    player.opLoc1(shortcutLoc(loc, near))
    val ticks = mutableListOf<ShortcutTick>()
    repeat(MAX_CROSSING_TICKS) {
        advance()
        val busy = player.vars[BUSY] != 0
        val mesbox = player.ui.containsModal(MESSAGE_BOX)
        ticks += ShortcutTick(player.coords, busy, agilityXp(), gameMessages(), mesbox)
        val started = ticks.indexOfFirst { it.busy }
        if (started >= 0 && !busy) {
            return ticks.drop(started)
        }
    }
    error("$loc did not finish in $MAX_CROSSING_TICKS ticks: ${ticks.map { it.coords }}")
}

/** Clicks [loc] and returns the game messages of the next ticks, for a refused shortcut. */
internal fun GameTestScope.refuseShortcut(
    loc: String,
    near: CoordGrid = player.coords,
): List<String> {
    val start = player.coords
    val before = agilityXp()
    player.opLoc1(shortcutLoc(loc, near))
    val messages = advanceCollecting(REFUSAL_TICKS)
    check(start == player.coords) { "$loc moved the player to ${player.coords}" }
    check(before == agilityXp()) { "$loc paid xp" }
    return messages
}

internal fun GameTestScope.setShortcutHitpoints(level: Int) {
    player.statMap.setBaseLevel("stat.hitpoints", level.toByte())
    player.statMap.setCurrentLevel("stat.hitpoints", level.toByte())
}

private const val MAX_CROSSING_TICKS = 40
private const val REFUSAL_TICKS = 4
private const val MESSAGE_BOX = "interface.messagebox"

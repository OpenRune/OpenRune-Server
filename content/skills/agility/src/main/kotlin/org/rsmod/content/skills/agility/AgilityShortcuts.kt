package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.sign
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

private const val SQUEEZE = "seq.human_squeeze"
private const val RAILING_SQUEEZE = "seq.railing_squeeze"
private const val PIPE_SQUEEZE = "seq.human_doublepipesqueeze"
private const val HOLE_SQUEEZE = "seq.hole_squeeze"
private const val HURDLE_JUMP = "seq.human_jump_hurdle"
private const val WALL_CLIMB = "seq.human_walk_crumbledwall"
private const val CRAWL = "seq.human_crawling"
private const val BALANCE_WALK = "seq.human_walk_logbalance_loop"
private const val CLIMB_ROCKS = "seq.human_climbing"
private const val CLIMB_DOWN_ROCKS = "seq.human_climbing_down"

/**
 * A shortcut is bound by the [option] the wiki lists for it rather than by the first op on the loc,
 * so a loc that also has a Search or a Talk-to keeps them.
 */
data class Shortcut(
    val locs: List<String>,
    val level: Int,
    val xp: Double,
    val option: String,
    val anim: String,
    val ticks: Int = 2,
    val links: Map<CoordGrid, CoordGrid> = emptyMap(),
)

/**
 * The handful of shortcuts [AgilityShortcutTable] has no tiles for. They all cross to the far side
 * of an obstacle on one level, so the landing is derived; levels, xp and the op name are the wiki's.
 */
object AgilityShortcutData {
    val all: List<Shortcut> =
        listOf(
            Shortcut(
                locs = listOf("loc.burgh_agility_shortcut_fence"),
                level = 25,
                xp = 0.0,
                option = "Jump-over",
                anim = HURDLE_JUMP,
            ),
            Shortcut(
                locs = listOf("loc.slayer_dungeon_floor_spikes_sc"),
                level = 43,
                xp = 5.0,
                option = "Jump-over",
                anim = HURDLE_JUMP,
            ),
            Shortcut(
                locs = listOf("loc.bush_shortcut"),
                level = 64,
                xp = 2.0,
                option = "Crawl-through",
                anim = CRAWL,
            ),
            Shortcut(
                locs = listOf("loc.wilderness_slayer_cave_crevice"),
                level = 77,
                xp = 10.0,
                option = "Squeeze-Through",
                anim = SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.prif_slayer_dungeon_shortcut_1a", "loc.prif_slayer_dungeon_shortcut_1b"),
                level = 78,
                xp = 1.0,
                option = "Pass",
                anim = SQUEEZE,
            ),
            Shortcut(
                locs =
                    listOf(
                        "loc.prif_slayer_dungeon_shortcut_2a",
                        "loc.prif_slayer_dungeon_shortcut_2b",
                    ),
                level = 84,
                xp = 1.5,
                option = "Pass",
                anim = SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.dagannoth_crevice"),
                level = 81,
                xp = 10.0,
                option = "Squeeze-Through",
                anim = SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.deepfin_cave_shortcut"),
                level = 84,
                xp = 0.0,
                option = "Squeeze-through",
                anim = SQUEEZE,
            ),
        )
}

/**
 * Every shortcut whose two ends are recorded in `agility-shortcuts.tsv`. Rows are grouped by loc and
 * op, so one obstacle with six approach tiles is one binding holding six links.
 */
object AgilityShortcutTable {
    private const val RESOURCE = "/agility-shortcuts.tsv"

    val rows: List<Shortcut> by lazy { parse(read()) }

    private fun read(): String =
        AgilityShortcutTable::class
            .java
            .getResourceAsStream(RESOURCE)
            ?.bufferedReader()
            ?.use { it.readText() } ?: ""

    private fun parse(text: String): List<Shortcut> {
        val links = LinkedHashMap<Pair<String, String>, LinkedHashMap<CoordGrid, CoordGrid>>()
        val details = HashMap<Pair<String, String>, Triple<Int, Double, Int>>()
        for (line in text.lineSequence()) {
            if (line.isBlank() || line.startsWith("#")) {
                continue
            }
            val cells = line.split('	')
            if (cells.size < 7) {
                continue
            }
            val key = cells[0] to cells[1]
            details.putIfAbsent(key, Triple(cells[2].toInt(), cells[3].toDouble(), cells[4].toInt()))
            links.getOrPut(key) { LinkedHashMap() }[coord(cells[5])] = coord(cells[6])
        }
        return links.map { (key, pairs) ->
            val (level, xp, ticks) = details.getValue(key)
            val (loc, option) = key
            Shortcut(listOf(loc), level, xp, option, animFor(option), ticks, pairs)
        }
    }

    private fun coord(text: String): CoordGrid {
        val (x, z, level) = text.split(',').map(String::toInt)
        return CoordGrid(x, z, level)
    }

    private fun animFor(option: String): String =
        when {
            option.startsWith("Squeeze", ignoreCase = true) -> SQUEEZE
            option.startsWith("Crawl", ignoreCase = true) -> CRAWL
            option.startsWith("Jump", ignoreCase = true) -> HURDLE_JUMP
            option.startsWith("Cross", ignoreCase = true) -> BALANCE_WALK
            option.startsWith("Walk", ignoreCase = true) -> BALANCE_WALK
            option.startsWith("Step", ignoreCase = true) -> HURDLE_JUMP
            option.startsWith("Climb-over", ignoreCase = true) -> WALL_CLIMB
            option.startsWith("Climb-down", ignoreCase = true) -> CLIMB_DOWN_ROCKS
            else -> CLIMB_ROCKS
        }
}

class AgilityShortcuts
@Inject
constructor(private val collision: CollisionFlagMap, private val xpMods: XpModifiers) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (shortcut in AgilityShortcutData.all + AgilityShortcutTable.rows) {
            for (loc in shortcut.locs) {
                val type = ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)) ?: continue
                val slot =
                    (1..5).firstOrNull {
                        type.actions.getOpOrNull(it - 1).equals(shortcut.option, ignoreCase = true)
                    } ?: continue
                when (slot) {
                    1 -> onOpLoc1(loc) { cross(it.loc, shortcut) }
                    2 -> onOpLoc2(loc) { cross(it.loc, shortcut) }
                    3 -> onOpLoc3(loc) { cross(it.loc, shortcut) }
                    4 -> onOpLoc4(loc) { cross(it.loc, shortcut) }
                    else -> onOpLoc5(loc) { cross(it.loc, shortcut) }
                }
            }
        }
    }

    private suspend fun ProtectedAccess.cross(loc: BoundLocInfo, shortcut: Shortcut) {
        if (player.agilityLvl < shortcut.level) {
            mes("You need an Agility level of ${shortcut.level} to use this shortcut.")
            return
        }

        val dest = shortcut.links[player.coords] ?: farSide(loc)
        if (dest == null) {
            mes("You can't find a way through from here.")
            return
        }

        faceSquare(loc.coords)
        anim(shortcut.anim)
        delay(shortcut.ticks)
        teleport(dest, TeleportType.Exempt)
        resetAnim()

        if (shortcut.xp > 0) {
            statAdvance(STAT_AGILITY, shortcut.xp * xpMods.get(player, STAT_AGILITY))
        }
    }

    /**
     * The first standable tile past [loc], straight through it from where the player stands.
     */
    private fun ProtectedAccess.farSide(loc: BoundLocInfo): CoordGrid? {
        val candidates =
            crossingCandidates(
                from = player.coords,
                loc = loc.coords,
                width = loc.adjustedWidth,
                length = loc.adjustedLength,
                angle = loc.angle,
                depth = SEARCH_DEPTH,
            )
        return candidates.firstOrNull { candidate ->
            val flags = collision[candidate.x, candidate.z, candidate.level]
            flags and CollisionFlag.BLOCK_WALK == 0
        }
    }

    private companion object {
        const val SEARCH_DEPTH = 2
    }
}

/**
 * The tiles to try landing on when crossing a loc, nearest first: the tile just past the loc's
 * footprint along the approach, then [depth] more behind it in case that one is taken up by
 * scenery. Taking the distance from the footprint rather than fixing it is what lets one rule serve
 * both a railing sitting on the player's own tile and a hole four tiles deep, so no shortcut needs
 * its landing tile recorded by hand.
 */
internal fun crossingCandidates(
    from: CoordGrid,
    loc: CoordGrid,
    width: Int,
    length: Int,
    angle: LocAngle,
    depth: Int,
): List<CoordGrid> {
    val centreX = loc.x + (width - 1) / 2.0
    val centreZ = loc.z + (length - 1) / 2.0
    val dx = centreX - from.x
    val dz = centreZ - from.z

    // A wall sits on the tile the player is standing on, so there is no direction to read off the
    // two positions; the wall's own angle is the way through.
    val (stepX, stepZ) =
        if (dx == 0.0 && dz == 0.0) {
            when (angle) {
                LocAngle.West -> -1 to 0
                LocAngle.East -> 1 to 0
                LocAngle.North -> 0 to 1
                LocAngle.South -> 0 to -1
            }
        } else if (abs(dx) >= abs(dz)) {
            sign(dx).toInt() to 0
        } else {
            0 to sign(dz).toInt()
        }
    val past =
        when {
            stepX > 0 -> loc.x + width - from.x
            stepX < 0 -> from.x - loc.x + 1
            stepZ > 0 -> loc.z + length - from.z
            else -> from.z - loc.z + 1
        }

    val first = maxOf(past, 1)
    return (first..first + depth).map { from.translate(stepX * it, stepZ * it) }
}

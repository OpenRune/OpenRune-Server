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
)

/**
 * Levels, xp and the op name are the wiki's; every loc was resolved back to this cache by id. Only
 * shortcuts that cross to the far side of the obstacle on one level are here - climbs, grapples and
 * stepping stone chains move the player somewhere no rule can derive, so they wait for a survey.
 */
object AgilityShortcutData {
    val all: List<Shortcut> =
        listOf(
            Shortcut(
                locs = listOf("loc.fai_falador_castle_crumble_mid"),
                level = 5,
                xp = 0.5,
                option = "Climb-over",
                anim = WALL_CLIMB,
            ),
            Shortcut(
                locs = listOf("loc.lumbridge_sc_fencejump"),
                level = 13,
                xp = 0.0,
                option = "Jump-over",
                anim = HURDLE_JUMP,
            ),
            Shortcut(
                locs = listOf("loc.burthorpe_diary_shortcut"),
                level = 14,
                xp = 0.0,
                option = "Manoeuvre-past",
                anim = SQUEEZE,
            ),
            // One loc id covers both Catacombs cracks, and the wiki puts the northern one at 34.
            // Binding it once means the northern crack opens seventeen levels early.
            Shortcut(
                locs = listOf("loc.zeah_cata_crack"),
                level = 17,
                xp = 0.0,
                option = "Squeeze-through",
                anim = SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.slayertower_window_shortcut_through"),
                level = 18,
                xp = 3.0,
                option = "Climb-through",
                anim = HOLE_SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.karam_dungeon_pipe2"),
                level = 22,
                xp = 8.5,
                option = "Squeeze-through",
                anim = PIPE_SQUEEZE,
                ticks = 3,
            ),
            Shortcut(
                locs = listOf("loc.av_lowwall_climb_1", "loc.av_lowwall_climb_2"),
                level = 24,
                xp = 6.0,
                option = "Climb-over",
                anim = WALL_CLIMB,
            ),
            Shortcut(
                locs = listOf("loc.burgh_agility_shortcut_fence"),
                level = 25,
                xp = 0.0,
                option = "Jump-over",
                anim = HURDLE_JUMP,
            ),
            Shortcut(
                locs = listOf("loc.dwarf_mines_sc_wall_crack"),
                level = 42,
                xp = 0.0,
                option = "Squeeze-through",
                anim = SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.slayer_dungeon_floor_spikes_sc"),
                level = 43,
                xp = 5.0,
                option = "Jump-over",
                anim = HURDLE_JUMP,
            ),
            Shortcut(
                locs = listOf("loc.varrock_dungeon_pipe_sc"),
                level = 51,
                xp = 10.0,
                option = "Squeeze-through",
                anim = PIPE_SQUEEZE,
                ticks = 3,
            ),
            Shortcut(
                locs = listOf("loc.slayer_dungeon_2_sc_wall_crack"),
                level = 61,
                xp = 0.0,
                option = "Squeeze-through",
                anim = SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.deepdungeonlooserailing"),
                level = 63,
                xp = 0.0,
                option = "Squeeze-through",
                anim = RAILING_SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.hosdun_agility_shortcut"),
                level = 63,
                xp = 10.0,
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
                locs = listOf("loc.morytania_railing_sc_fence_1", "loc.morytania_railing_sc_fence_2"),
                level = 65,
                xp = 0.0,
                option = "Squeeze-through",
                anim = RAILING_SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.taverly_dungeon_pipe_sc"),
                level = 70,
                xp = 10.0,
                option = "Squeeze-through",
                anim = PIPE_SQUEEZE,
                ticks = 3,
            ),
            Shortcut(
                locs = listOf("loc.fossil_shortcut_basecamp_a", "loc.fossil_shortcut_basecamp_b"),
                level = 70,
                xp = 0.0,
                option = "Climb through",
                anim = HOLE_SQUEEZE,
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
                locs = listOf("loc.taverly_dungeon_floor_spikes_sc"),
                level = 80,
                xp = 12.5,
                option = "Jump-over",
                anim = HURDLE_JUMP,
            ),
            Shortcut(
                locs = listOf("loc.dagannoth_crevice"),
                level = 81,
                xp = 10.0,
                option = "Squeeze-Through",
                anim = SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.prif_slayer_dungeon_shortcut_2a", "loc.prif_slayer_dungeon_shortcut_2b"),
                level = 84,
                xp = 1.5,
                option = "Pass",
                anim = SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.deepfin_cave_shortcut"),
                level = 84,
                xp = 0.0,
                option = "Squeeze-through",
                anim = SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.kalphite_wall_shortcut"),
                level = 86,
                xp = 0.0,
                option = "Squeeze-through",
                anim = SQUEEZE,
            ),
            Shortcut(
                locs = listOf("loc.darkm_wall_rock_shortcut"),
                level = 86,
                xp = 0.0,
                option = "Jump-over",
                anim = HURDLE_JUMP,
            ),
            Shortcut(
                locs = listOf("loc.legends_quest_cave_shortcut"),
                level = 96,
                xp = 7.5,
                option = "Squeeze-Through",
                anim = SQUEEZE,
            ),
        )
}

class AgilityShortcuts
@Inject
constructor(private val collision: CollisionFlagMap, private val xpMods: XpModifiers) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (shortcut in AgilityShortcutData.all) {
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

        val dest = farSide(loc)
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

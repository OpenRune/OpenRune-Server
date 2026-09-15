package org.rsmod.content.skills.agility

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.sign
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.stat.rangedLvl
import org.rsmod.api.player.stat.strengthLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag
import skillSuccess

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
    val links: Map<CoordGrid, ShortcutLink> = emptyMap(),
    val fail: ShortcutFail? = null,
)

/**
 * One crossing of an obstacle. The level and the requirements sit on the link rather than the
 * shortcut because an obstacle can be two shortcuts wearing one loc id: both Catacombs of Kourend
 * cracks are `loc.zeah_cata_crack`, and the northern one wants seventeen more levels.
 */
data class ShortcutLink(val dest: CoordGrid, val level: Int, val reqs: ShortcutReqs)

/**
 * A crossing that can go wrong. [low] and [high] are the wiki's level-1 and level-99 odds out of
 * 256, run through the same skilling formula as everything else; a failed attempt still pays [xp]
 * and deals [damage], and leaves the player where they started.
 */
data class ShortcutFail(val low: Int, val high: Int, val xp: Double, val damage: IntRange?) {
    companion object {
        fun parse(text: String): ShortcutFail? {
            val parts = text.split('/')
            if (parts.size < 3) {
                return null
            }
            val low = parts[0].toIntOrNull() ?: return null
            val high = parts[1].toIntOrNull() ?: return null
            val damage =
                parts.getOrNull(3)?.takeIf { it.isNotBlank() }?.let {
                    val range = it.split('-')
                    range[0].toInt()..range[1].toInt()
                }
            return ShortcutFail(low, high, parts[2].toDoubleOrNull() ?: 0.0, damage)
        }
    }
}

/**
 * Everything a crossing asks for beyond the Agility level. [bareLevel] is the alternative live
 * offers on the grapple crossings: the same gap, no crossbow, a much higher Agility level.
 */
data class ShortcutReqs(
    val ranged: Int = 0,
    val strength: Int = 0,
    val gear: Gear? = null,
    val quest: String? = null,
    val varSymbol: String? = null,
    val varValue: Int = 0,
    val varExact: Boolean = false,
    val bareLevel: Int = 0,
) {
    enum class Gear {
        Grapple,
        ClimbingBoots,
    }

    companion object {
        val NONE: ShortcutReqs = ShortcutReqs()

        fun parse(text: String): ShortcutReqs {
            var reqs = NONE
            for (part in text.split(';')) {
                if (part.isBlank()) {
                    continue
                }
                val key = part.substringBefore('=')
                val value = part.substringAfter('=')
                reqs =
                    when (key) {
                        "ranged" -> reqs.copy(ranged = value.toInt())
                        "strength" -> reqs.copy(strength = value.toInt())
                        "gear" ->
                            reqs.copy(
                                gear = if (value == "grapple") Gear.Grapple else Gear.ClimbingBoots
                            )
                        "bare" -> reqs.copy(bareLevel = value.toInt())
                        "quest" -> reqs.copy(quest = value)
                        "var" -> reqs.varGate(value)
                        else -> reqs
                    }
            }
            return reqs
        }

        private fun ShortcutReqs.varGate(gate: String): ShortcutReqs {
            val exact = !gate.contains(">=")
            val separator = if (exact) "=" else ">="
            val symbol = gate.substringBefore(separator)
            val value = gate.substringAfter(separator).toIntOrNull() ?: return this
            return copy(varSymbol = symbol, varValue = value, varExact = exact)
        }
    }
}

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
        val links =
            LinkedHashMap<Pair<String, String>, LinkedHashMap<CoordGrid, ShortcutLink>>()
        val details = HashMap<Pair<String, String>, Triple<Int, Double, Int>>()
        val fails = HashMap<Pair<String, String>, ShortcutFail>()
        for (line in text.lineSequence()) {
            if (line.isBlank() || line.startsWith("#")) {
                continue
            }
            val cells = line.split('	')
            if (cells.size < 7) {
                continue
            }
            val key = cells[0] to cells[1]
            val level = cells[2].toInt()
            details.putIfAbsent(key, Triple(level, cells[3].toDouble(), cells[4].toInt()))
            val reqs = if (cells.size > 7) ShortcutReqs.parse(cells[7]) else ShortcutReqs.NONE
            links.getOrPut(key) { LinkedHashMap() }[coord(cells[5])] =
                ShortcutLink(coord(cells[6]), level, reqs)
            if (cells.size > 8 && cells[8].isNotBlank()) {
                ShortcutFail.parse(cells[8])?.let { fails[key] = it }
            }
        }
        return links.map { (key, pairs) ->
            val (level, xp, ticks) = details.getValue(key)
            val (loc, option) = key
            Shortcut(listOf(loc), level, xp, option, animFor(option), ticks, pairs, fails[key])
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
                val type = ServerCacheManager.getObject(locId(loc) ?: continue) ?: continue
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
        val link = shortcut.links[player.coords]
        val level = link?.level ?: shortcut.level
        if (player.agilityLvl < level) {
            mes("You need an Agility level of $level to use this shortcut.")
            return
        }

        if (!meets(link?.reqs ?: ShortcutReqs.NONE)) {
            return
        }

        val dest = link?.dest ?: farSide(loc)
        if (dest == null) {
            mes("You can't find a way through from here.")
            return
        }

        faceSquare(loc.coords)
        anim(shortcut.anim)
        delay(shortcut.ticks)

        val failed = shortcut.fail?.let { !skillSuccess(it.low, it.high, player.agilityLvl) } == true
        if (failed) {
            slip(shortcut.fail!!)
            return
        }

        teleport(dest, TeleportType.Exempt)
        resetAnim()

        if (shortcut.xp > 0) {
            statAdvance(STAT_AGILITY, shortcut.xp * xpMods.get(player, STAT_AGILITY))
        }
    }

    /** A failed crossing pays its own xp and hurts, and leaves the player on the side they started. */
    private fun ProtectedAccess.slip(fail: ShortcutFail) {
        resetAnim()
        mes("You lose your footing and fail to make it across.")
        val damage = fail.damage
        if (damage != null) {
            queueHit(delay = 0, type = HitType.Typeless, damage = random.of(damage.first, damage.last))
        }
        if (fail.xp > 0) {
            statAdvance(STAT_AGILITY, fail.xp * xpMods.get(player, STAT_AGILITY))
        }
    }

    /**
     * Messages and returns false on the first requirement the player is short of. Quest gates go
     * through [QuestRequirements] rather than reading the quest var, so they follow whichever mode
     * the realm runs in; everything else is the comparison the data carries.
     */
    private fun ProtectedAccess.meets(reqs: ShortcutReqs): Boolean {
        val barehanded = reqs.bareLevel > 0 && player.agilityLvl >= reqs.bareLevel
        if (!barehanded) {
            if (reqs.ranged > 0 && player.rangedLvl < reqs.ranged) {
                mes("You need a Ranged level of ${reqs.ranged} to use this shortcut.")
                return false
            }
            if (reqs.strength > 0 && player.strengthLvl < reqs.strength) {
                mes("You need a Strength level of ${reqs.strength} to use this shortcut.")
                return false
            }
            when (reqs.gear) {
                ShortcutReqs.Gear.Grapple ->
                    if (!wearingGrapple()) {
                        mes("You need a crossbow and a mith grapple to use this shortcut.")
                        return false
                    }
                ShortcutReqs.Gear.ClimbingBoots ->
                    if (player.worn[Wearpos.Feet.slot]?.isType(CLIMBING_BOOTS) != true) {
                        mes("You need climbing boots to use this shortcut.")
                        return false
                    }
                null -> Unit
            }
        }
        val quest = reqs.quest
        if (quest != null && !QuestRequirements.hasCompleted(player, quest)) {
            mes("You need to have completed a quest to use this shortcut.")
            return false
        }
        val symbol = reqs.varSymbol
        if (symbol != null) {
            val current = player.vars[symbol]
            val satisfied = if (reqs.varExact) current == reqs.varValue else current >= reqs.varValue
            if (!satisfied) {
                mes("You can't use this shortcut yet.")
                return false
            }
        }
        return true
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
        const val CLIMBING_BOOTS = "obj.death_climbingboots"
    }
}

private const val MITH_GRAPPLE = "obj.xbows_grapple_tip_bolt_mithril_rope"

/**
 * The shortcut table is data, and a symbol in it that no longer resolves is a bad row rather than a
 * reason to take the server down: `asRSCM` throws, and thrown out of `startup` it aborts the whole
 * plugin load. One mistyped name cost three corrupted rows a boot before this was here.
 */
internal fun locId(loc: String): Int? {
    val id = runCatching { loc.asRSCM(RSCMType.LOC) }.getOrNull()
    if (id == null) {
        logger.warn { "Agility: no such loc '$loc', skipping it." }
    }
    return id
}

private val logger = InlineLogger()

/** Any crossbow in hand with a mith grapple in the quiver, which is what live asks for. */
internal fun ProtectedAccess.wearingGrapple(): Boolean {
    if (player.worn[Wearpos.Quiver.slot]?.isType(MITH_GRAPPLE) != true) {
        return false
    }
    val weapon = player.worn[Wearpos.RightHand.slot] ?: return false
    val name = ServerCacheManager.getItem(weapon.id)?.name ?: return false
    return name.contains("crossbow", ignoreCase = true)
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

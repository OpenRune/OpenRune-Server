package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.table.HerbiboarFossilsRow
import org.rsmod.api.table.HerbiboarHerbsRow
import org.rsmod.api.table.HerbiboarLegsRow
import org.rsmod.api.table.HerbiboarSpotsRow
import org.rsmod.api.table.HerbiboarTunnelsRow
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HerbiboarScript
@Inject
constructor(
    private val objRepo: ObjRepository,
    private val random: GameRandom,
    private val rumours: HunterRumours,
) : PluginScript() {
    private val legs: List<Leg> by lazy { loadLegs() }
    private val spotGroups: Map<Int, Int> by lazy {
        HerbiboarSpotsRow.all().associate { it.loc.id to it.group }
    }
    private val tunnels: Map<Int, org.rsmod.map.CoordGrid> by lazy {
        HerbiboarTunnelsRow.all().associate { it.index to it.coords }
    }
    private val trailVarbits: List<VarBitType> by lazy { legs.map { it.trail }.distinctBy { it.id } }

    override fun ScriptContext.startup() {
        for (start in START_LOCS) {
            onOpLoc1(start) { inspectStart(it.loc.id) }
            onOpLoc2(start) { mes("You have harvested ${vars[HARVESTED]} Herbiboars.") }
            onOpLoc3(start) { toggleWarning() }
        }
        for (row in HerbiboarSpotsRow.all()) {
            onOpLoc1(row.loc.internalName) { inspectSpot(it.loc.id) }
        }
        onOpLoc1(TUNNEL) { attackTunnel(it.loc.coords) }
        onOpLoc2(TUNNEL) { attackTunnel(it.loc.coords) }
        onOpNpc1(HERBIBOAR) { harvest() }
    }

    suspend fun ProtectedAccess.inspectStart(loc: Int) {
        if (stat(HUNTER) < LEVEL) {
            mes("You need a Hunter level of $LEVEL to track a herbiboar.")
            return
        }
        anim(INSPECT_SEQ)
        delay(INSPECT_TICKS)
        reset(player)
        val first = legs.filter { it.from == loc }.randomOrNull(random) ?: return
        VarPlayerIntMapSetter.set(player, ROUTE_LENGTH, random.of(MIN_LEGS, MAX_LEGS))
        reveal(player, first)
        VarPlayerIntMapSetter.set(player, STARTED, 1)
        mes("You find some fresh tracks.")
        if (!continueFrom(player, first)) confuse()
    }

    suspend fun ProtectedAccess.inspectSpot(loc: Int) {
        anim(INSPECT_SEQ)
        delay(INSPECT_TICKS)
        val leg = legs.firstOrNull { it.from == loc && player.vars[it.trail] == it.dir }
        if (leg == null) {
            mes("Nothing seems to be out of place here.")
            return
        }
        reveal(player, leg)
        mes(SPOT_MESSAGES[locName(loc)] ?: "Something has passed this way.")
        if (isLast(player, leg)) {
            VarPlayerIntMapSetter.set(player, ENDS, leg.endTunnel)
            mes("The creature's tracks lead into a nearby tunnel.")
            return
        }
        statAdvance(HUNTER, INSPECT_XP)
        rollFossil()
        if (!continueFrom(player, leg)) confuse()
    }

    suspend fun ProtectedAccess.attackTunnel(coords: org.rsmod.map.CoordGrid) {
        val index = tunnels.entries.firstOrNull { it.value == coords }?.key ?: return
        if (vars[ENDS] != index) {
            mes("Nothing seems to be hiding in this tunnel.")
            return
        }
        anim(KICK_SEQ)
        delay(INSPECT_TICKS)
        if (vars[ENDS] != index) return
        reset(player)
        VarPlayerIntMapSetter.set(player, VISIBLE, index)
        statAdvance(HUNTER, tunnelXp(statBase(HUNTER)).toDouble())
        mes("You stun the herbiboar.")
    }

    suspend fun ProtectedAccess.harvest() {
        if (vars[VISIBLE] == 0) return
        if (stat(HERBLORE) < HERBLORE_LEVEL) {
            mes("You need a Herblore level of $HERBLORE_LEVEL to harvest from the herbiboar.")
            return
        }
        anim(HARVEST_SEQ)
        delay(INSPECT_TICKS)
        if (vars[VISIBLE] == 0) return
        val secateurs = SECATEURS in inv || SECATEURS in player.worn
        val count = random.of(1, 3) + if (secateurs) 1 else 0
        val herb = rollHerb(stat(HERBLORE)) { random.of(it) }
        VarPlayerIntMapSetter.set(player, VISIBLE, 0)
        invAddOrDrop(objRepo, herb, count)
        if (count > 1) statAdvance(HERBLORE, HERB_XP * (count - 1))
        VarPlayerIntMapSetter.set(player, HARVESTED, vars[HARVESTED] + 1)
        mes("You harvest herbs from the herbiboar, whereupon it escapes.")
        rumours.onCatch(player, HERBIBOAR)
    }

    private fun ProtectedAccess.toggleWarning() {
        val ignore = vars[IGNORE_WARNING] == 0
        VarPlayerIntMapSetter.set(player, IGNORE_WARNING, if (ignore) 1 else 0)
        if (ignore) {
            mes("You will no longer be warned if you try to track a second herbiboar before harvesting a caught one.")
        } else {
            mes("You will now be warned if you try to track a second herbiboar before harvesting a caught one.")
        }
    }

    private fun ProtectedAccess.confuse() {
        reset(player)
        mes(
            "The creature has successfully confused you with its tracks, leading you round in " +
                "circles. You'll need to start again."
        )
    }

    private fun ProtectedAccess.rollFossil() {
        val roll = random.of(FOSSIL_ROLL)
        var edge = 0
        for (fossil in HerbiboarFossilsRow.all()) {
            edge += fossil.weight
            if (roll < edge) {
                invAddOrDrop(objRepo, fossil.fossil.internalName, random.of(fossil.min, fossil.max))
                return
            }
        }
    }

    private fun continueFrom(player: Player, leg: Leg): Boolean {
        val group = leg.toGroup ?: return true
        val used = trailVarbits.filter { player.vars[it] != 0 }.map { it.id }.toSet()
        val candidates =
            legs.filter { spotGroups[it.from] == group && it.trail.id !in used && it.trail.id != leg.trail.id }
        if (candidates.isEmpty()) return false
        val revealed = revealedCount(player)
        val wantEnd = revealed + 1 >= player.vars[ROUTE_LENGTH]
        val preferred = candidates.filter { (it.endTunnel > 0) == wantEnd }.ifEmpty { candidates }
        val next = preferred.randomOrNull(random) ?: return false
        VarPlayerIntMapSetter.set(player, next.trail, next.dir)
        return true
    }

    private fun isLast(player: Player, leg: Leg): Boolean =
        leg.toGroup == null || (leg.endTunnel > 0 && revealedCount(player) >= player.vars[ROUTE_LENGTH])

    private fun revealedCount(player: Player): Int = trailVarbits.count { player.vars[it] >= REVEALED }

    private fun reveal(player: Player, leg: Leg) {
        for (varbit in trailVarbits) {
            val value = player.vars[varbit]
            if (value in REVEALED until FADED) VarPlayerIntMapSetter.set(player, varbit, value + 2)
        }
        VarPlayerIntMapSetter.set(player, leg.trail, leg.dir + 2)
    }

    private fun reset(player: Player) {
        for (varbit in trailVarbits) VarPlayerIntMapSetter.set(player, varbit, 0)
        VarPlayerIntMapSetter.set(player, ENDS, 0)
        VarPlayerIntMapSetter.set(player, STARTED, 0)
        VarPlayerIntMapSetter.set(player, VISIBLE, 0)
    }

    private fun <T> List<T>.randomOrNull(random: GameRandom): T? = if (isEmpty()) null else this[random.of(size)]

    private fun locName(id: Int): String = ServerCacheManager.getObject(id)?.name.orEmpty()

    data class Leg(val from: Int, val trail: VarBitType, val dir: Int, val toGroup: Int?, val endTunnel: Int)

    private fun loadLegs(): List<Leg> =
        HerbiboarLegsRow.all().map {
            Leg(
                from = it.fromLoc.id,
                trail = checkNotNull(ServerCacheManager.getVarbit(it.trail)) { "Missing trail varbit ${it.trail}" },
                dir = it.dir,
                toGroup = it.toGroup.takeIf { group -> group >= 0 },
                endTunnel = it.endTunnel,
            )
        }

    companion object {
        const val HUNTER = "stat.hunter"
        const val HERBLORE = "stat.herblore"
        const val LEVEL = 80
        const val HERBLORE_LEVEL = 31
        const val INSPECT_XP = 50.0
        const val HERB_XP = 25.0
        const val MIN_LEGS = 3
        const val MAX_LEGS = 5
        const val REVEALED = 3
        const val FADED = 5
        private const val INSPECT_TICKS = 2

        const val TUNNEL = "loc.hunting_trail_end_fossil"
        const val HERBIBOAR = "npc.fossil_herbiboar_visible"
        const val SECATEURS = "obj.fairy_enchanted_secateurs"
        const val ENDS = "varbit.hunting_trail_ends_fossil"
        const val STARTED = "varbit.hunting_trails_used_fossil"
        const val VISIBLE = "varbit.fossil_herbiboar_visible"
        const val IGNORE_WARNING = "varbit.fossil_herbiboar_already_caught_ignore_warning"
        const val HARVESTED = "varp.herbiboar_harvested"
        const val ROUTE_LENGTH = "varp.herbiboar_route_length"
        private const val INSPECT_SEQ = "seq.picking_low"
        private const val HARVEST_SEQ = "seq.picking_low"
        private const val KICK_SEQ = "seq.human_kicks_chest_open"

        val START_LOCS = (1..5).map { "loc.hunting_trail_spawn_fossil$it" }

        val SPOT_MESSAGES =
            mapOf(
                "Muddy patch" to "You find a partial footprint in the disturbed earth. Something has passed this way.",
                "Smelly mushroom" to "This mushroom is oozing where it has recently been bitten. Something has passed this way.",
                "Seaweed" to "Something has disturbed this seaweed recently, it smells.",
                "Mushroom" to "The mushroom cap looks freshly eaten. Something has passed this way.",
            )

        private const val FOSSIL_ROLL = 3500

        fun tunnelXp(level: Int): Int =
            when {
                level < 80 -> 1950
                level <= 94 -> 1950 + 30 * (level - 80)
                else -> 2385 + 19 * (level.coerceAtMost(99) - 95)
            }

        fun herbChance(level: Int, low: Int, high: Int): Int =
            (low * (99 - level) / 98 + high * (level - 1) / 98 + 1).coerceIn(0, 256)

        fun rollHerb(level: Int, nextInt: (Int) -> Int): String {
            val rows = HerbiboarHerbsRow.all()
            for (row in rows) {
                if (nextInt(256) < herbChance(level, row.low, row.high)) return row.herb.internalName
            }
            return rows.last().herb.internalName
        }
    }
}

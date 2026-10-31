package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.table.hunter.HunterTrapCreaturesRow
import org.rsmod.api.table.hunter.HunterTrapsRow
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class HunterTrapScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
    private val clock: MapClock,
    private val rumours: HunterRumours,
) : PluginScript() {
    private val traps = HashMap<CoordGrid, Trap>()
    private val kinds: List<TrapKind> by lazy { loadKinds() }

    override fun ScriptContext.startup() {
        for (kind in kinds) {
            onOpHeld1(kind.item) { lay(kind) }
            onOpLoc1(kind.setLoc) { dismantle(it.loc.coords) }
            onOpLoc2(kind.setLoc) { investigate(it.loc.coords) }
            onOpLoc1(kind.brokenLoc) { dismantle(it.loc.coords) }
            onOpLoc2(kind.brokenLoc) { reset(it.loc.coords) }
            for (creature in kind.creatures) {
                onOpLoc1(creature.fullLoc) { check(it.loc.coords) }
                onOpLoc2(creature.fullLoc) { reset(it.loc.coords) }
            }
        }
        onPlayerSoftTimer(TIMER) { player.trapTick() }
        onPlayerLogout { player.returnTraps() }
    }

    fun trapsOf(player: Player): List<Trap> = traps.values.filter { it.owner == player }

    fun trapAt(coords: CoordGrid): Trap? = traps[coords]

    fun kinds(): List<TrapKind> = kinds

    suspend fun ProtectedAccess.lay(kind: TrapKind) {
        val level = stat(HUNTER)
        if (level < kind.level) {
            mes("You need a Hunter level of ${kind.level} to set up a ${kind.name}.")
            return
        }
        val limit = maxTraps(level)
        if (trapsOf(player).size >= limit) {
            val plural = if (limit == 1) "trap" else "traps"
            mes("You don't have a high enough Hunter level to set up more than $limit $plural.")
            return
        }
        val tile = player.coords
        if (traps.containsKey(tile) || locRepo.findAll(tile).any { it.shape != LocShape.GroundDecor }) {
            mes("You can't lay a trap here.")
            return
        }
        val step = stepOffTile(tile)
        if (step == null) {
            mes("You can't lay a trap here.")
            return
        }
        mes("You begin setting up the trap.")
        anim(LAY_SEQ)
        delay(LAY_TICKS)
        if (player.coords != tile || traps.containsKey(tile)) return
        if (invDel(inv, kind.item).failure) return
        val loc = placeLoc(tile, kind.setLoc)
        traps[tile] = Trap(player, kind, tile, loc, TrapState.Set, null, clock.cycle)
        walk(step)
        player.softTimer(TIMER, 1)
    }

    private suspend fun ProtectedAccess.dismantle(coords: CoordGrid) {
        val trap = ownTrap(coords) ?: return
        if (trap.state != TrapState.Set && trap.state != TrapState.Broken) return
        if (!inv.hasFreeSpace() && inv.count(trap.kind.item) == 0) {
            mes("You don't have enough inventory space.")
            return
        }
        anim(LAY_SEQ)
        delay(1)
        if (traps[coords] !== trap) return
        removeTrap(trap)
        invAdd(inv, trap.kind.item)
        mes("You dismantle the trap.")
    }

    private fun ProtectedAccess.investigate(coords: CoordGrid) {
        val trap = ownTrap(coords) ?: return
        mes("This ${trap.kind.name} hasn't caught anything yet.")
    }

    suspend fun ProtectedAccess.check(coords: CoordGrid): Boolean {
        val trap = ownTrap(coords) ?: return false
        val creature = trap.creature
        if (trap.state != TrapState.Full || creature == null) return false
        val items = listOf(trap.kind.item to 1) + creature.loot.map { it.obj to random.of(it.min, it.max) }
        if (!canAdd(items)) {
            mes("You don't have enough inventory space.")
            return false
        }
        anim(LAY_SEQ)
        delay(1)
        if (traps[coords] !== trap || !canAdd(items)) return false
        removeTrap(trap)
        for ((obj, count) in items) invAdd(inv, obj, count)
        statAdvance(HUNTER, creature.xp)
        val caught = "You've caught ${creature.article} ${creature.name}"
        mes(if (creature.npc == JERBOA) "$caught... or maybe not." else "$caught.")
        rumours.onCatch(player, creature.npc)
        return true
    }

    private fun ProtectedAccess.canAdd(items: List<Pair<String, Int>>): Boolean {
        val into = inv
        return player
            .invTransaction(into, autoCommit = false) {
                val target = select(into)
                for ((obj, count) in items) add(target, obj.asRSCM(RSCMType.OBJ), count)
            }
            .success
    }

    private suspend fun ProtectedAccess.reset(coords: CoordGrid) {
        val trap = ownTrap(coords) ?: return
        when (trap.state) {
            TrapState.Full -> if (!check(coords)) return
            TrapState.Broken -> dismantle(coords)
            else -> return
        }
        if (traps.containsKey(coords) || trap.kind.item !in inv) return
        playerWalk(coords)
        if (player.coords != coords) return
        lay(trap.kind)
    }

    private fun ProtectedAccess.ownTrap(coords: CoordGrid): Trap? {
        val trap = traps[coords] ?: return null
        if (trap.owner != player) {
            mes("This isn't your trap.")
            return null
        }
        return trap
    }

    private fun Player.trapTick() {
        val own = trapsOf(this)
        if (own.isEmpty()) {
            clearSoftTimer(TIMER)
            return
        }
        val now = clock.cycle
        for (trap in own) {
            val age = now - trap.stateCycle
            when (trap.state) {
                TrapState.Set -> {
                    if (age >= COLLAPSE_TICKS) {
                        collapse(trap)
                    } else if (age > 0 && age % ATTEMPT_TICKS == 0) {
                        attempt(trap)
                    }
                }
                TrapState.Trapping -> if (age >= SETTLE_TICKS) setState(trap, TrapState.Full)
                TrapState.Failing -> if (age >= FAIL_TICKS) setState(trap, TrapState.Broken)
                TrapState.Full,
                TrapState.Broken -> if (age >= COLLAPSE_TICKS) collapse(trap)
            }
        }
    }

    private fun Player.attempt(trap: Trap) {
        if (coords == trap.coords) return
        val level = statBase(HUNTER)
        val candidates =
            trap.kind.creatures.filter { level >= it.level }.associateBy { it.npc.asRSCM(RSCMType.NPC) }
        val npc = nearbyCreature(trap.coords, candidates.keys) ?: return
        val creature = candidates.getValue(npc.id)
        if (random.of(256) < catchChance256(creature.low, creature.high, level)) {
            if (npc.respawns) npcRepo.despawn(npc, creature.respawnTicks) else npcRepo.del(npc, Int.MAX_VALUE)
            trap.creature = creature
            setState(trap, TrapState.Trapping)
        } else {
            setState(trap, TrapState.Failing)
        }
    }

    private fun nearbyCreature(center: CoordGrid, types: Set<Int>): Npc? =
        npcRepo
            .findAll(ZoneKey.from(center), zoneRadius = 1)
            .filter { it.id in types && it.isSlotAssigned && it.isVisible }
            .filter { it.coords.level == center.level }
            .filter { max(abs(it.coords.x - center.x), abs(it.coords.z - center.z)) <= RANGE }
            .minByOrNull { max(abs(it.coords.x - center.x), abs(it.coords.z - center.z)) }

    private fun setState(trap: Trap, state: TrapState) {
        val internal =
            when (state) {
                TrapState.Set -> trap.kind.setLoc
                TrapState.Trapping -> trap.creature!!.trappingLoc
                TrapState.Full -> trap.creature!!.fullLoc
                TrapState.Failing -> trap.kind.failingLoc
                TrapState.Broken -> trap.kind.brokenLoc
            }
        locRepo.del(trap.loc, Int.MAX_VALUE)
        trap.loc = placeLoc(trap.coords, internal)
        trap.state = state
        trap.stateCycle = clock.cycle
    }

    private fun Player.collapse(trap: Trap) {
        removeTrap(trap)
        objRepo.add(trap.kind.item, trap.coords, OBJ_DURATION, receiver = this)
        mes("Your trap has collapsed.")
    }

    private fun Player.returnTraps() {
        for (trap in trapsOf(this)) {
            removeTrap(trap)
            objRepo.add(trap.kind.item, trap.coords, OBJ_DURATION, receiver = this)
        }
    }

    private fun removeTrap(trap: Trap) {
        traps.remove(trap.coords)
        locRepo.del(trap.loc, Int.MAX_VALUE)
    }

    private fun placeLoc(coords: CoordGrid, internal: String): LocInfo =
        locRepo.add(coords, internal, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)

    private fun stepOffTile(from: CoordGrid): CoordGrid? =
        listOf(from.translate(-1, 0), from.translate(1, 0), from.translate(0, -1), from.translate(0, 1))
            .firstOrNull { !collision.isWalkBlocked(it) && !traps.containsKey(it) }

    private fun loadKinds(): List<TrapKind> {
        val creatures = HunterTrapCreaturesRow.all().groupBy { it.trap }
        return HunterTrapsRow.all().mapIndexed { index, row ->
            TrapKind(
                name = row.name,
                item = row.item.internalName,
                level = row.level,
                setLoc = locName(row.setLoc.id),
                failingLoc = locName(row.failingLoc.id),
                brokenLoc = locName(row.brokenLoc.id),
                creatures =
                    creatures[index].orEmpty().map { c ->
                        val loot = mutableListOf<Drop>()
                        if (index == BIRD_SNARE_INDEX) {
                            loot += Drop(BONES, 1)
                            loot += Drop(RAW_BIRD_MEAT, 1)
                        }
                        loot += Drop(c.loot.internalName, c.lootMin, c.lootMax)
                        Creature(
                            name = c.name,
                            npc = RSCM.getReverseMapping(RSCMType.NPC, c.npc.id),
                            level = c.level,
                            xp = c.xpTenths / 10.0,
                            trappingLoc = locName(c.trappingLoc.id),
                            fullLoc = locName(c.fullLoc.id),
                            loot = loot,
                            low = c.low,
                            high = c.high,
                            respawnTicks = c.respawnTicks,
                        )
                    },
            )
        }
    }

    private fun locName(id: Int): String = RSCM.getReverseMapping(RSCMType.LOC, id)

    enum class TrapState {
        Set,
        Trapping,
        Full,
        Failing,
        Broken,
    }

    class Trap(
        val owner: Player,
        val kind: TrapKind,
        val coords: CoordGrid,
        var loc: LocInfo,
        var state: TrapState,
        var creature: Creature?,
        var stateCycle: Int,
    )

    class Drop(val obj: String, val min: Int, val max: Int = min)

    class Creature(
        val name: String,
        val npc: String,
        val level: Int,
        val xp: Double,
        val trappingLoc: String,
        val fullLoc: String,
        val loot: List<Drop>,
        val low: Int,
        val high: Int,
        val respawnTicks: Int,
    ) {
        val article: String
            get() = if (name.first() in "aeiou") "an" else "a"
    }

    class TrapKind(
        val name: String,
        val item: String,
        val level: Int,
        val setLoc: String,
        val failingLoc: String,
        val brokenLoc: String,
        val creatures: List<Creature>,
    )

    companion object {
        const val HUNTER = "stat.hunter"
        const val TIMER = "timer.hunter_traps"
        const val JERBOA = "npc.varlamore_hunterjerboa01"
        const val LAY_SEQ = "seq.hunting_setting_trap_small"
        const val LAY_TICKS = 3
        const val ATTEMPT_TICKS = 3
        const val SETTLE_TICKS = 2
        const val FAIL_TICKS = 1
        const val RANGE = 2
        const val COLLAPSE_TICKS = 200
        const val OBJ_DURATION = 200
        const val BIRD_SNARE_INDEX = 0
        const val BONES = "obj.bones"
        const val RAW_BIRD_MEAT = "obj.spit_raw_bird_meat"

        fun maxTraps(level: Int): Int = 1 + listOf(20, 40, 60, 80).count { level >= it }

        fun catchChance256(low: Int, high: Int, level: Int): Int =
            (SkillingSuccessRate.successRate(low, high, level.coerceIn(1, 99), 99) * 256).roundToInt()
    }
}

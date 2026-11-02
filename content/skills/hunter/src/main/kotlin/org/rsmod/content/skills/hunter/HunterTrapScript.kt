package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.enums.enum
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpObj4
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.table.hunter.HunterTrapCreaturesRow
import org.rsmod.api.table.hunter.HunterTrapsRow
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.obj.Obj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal suspend fun ProtectedAccess.walkOnto(dest: CoordGrid): Boolean {
    walk(dest)
    while (coords != dest && player.routeDestination.isNotEmpty()) delay(1)
    return coords == dest
}

@Singleton
class HunterTrapScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val routes: RouteFactory,
    private val collision: CollisionFlagMap,
    private val areas: AreaChecker,
    private val random: GameRandom,
    private val clock: MapClock,
    private val rumours: HunterRumours,
    private val pets: PetRewards,
    private val limits: HunterTrapLimits,
    private val playerRepo: PlayerRepository,
) : PluginScript() {
    private val traps = HashMap<CoordGrid, Trap>()
    private val kinds: List<TrapKind> by lazy { loadKinds() }

    override fun ScriptContext.startup() {
        limits.register { player -> trapsOf(player).size }
        for (kind in kinds) {
            onOpHeld1(kind.item) { lay(kind, null) }
            onOpObj4(kind.itemType) { if (it.obj.coords == player.coords) lay(kind, it.obj) }
            onOpLoc1(kind.setLoc) { dismantle(it.loc.coords) }
            onOpLoc2(kind.setLoc) { investigate(it.loc.coords) }
            onOpLoc1(kind.brokenLoc) { dismantle(it.loc.coords) }
            onOpLoc2(kind.brokenLoc) { reset(it.loc.coords) }
            for (torch in TORCHES) onOpLocU(kind.setLoc, torch) { smoke(it.loc.coords) }
            for (loc in kind.creatures.flatMap { it.checkLocs }.distinct()) {
                onOpLoc1(loc) { check(it.loc.coords) }
                onOpLoc2(loc) { reset(it.loc.coords) }
            }
            for ((bait, leftover) in kind.creatures.flatMap { c -> c.baits.map { it to c.baitLeftover } }.distinct()) {
                onOpLocU(kind.setLoc, bait) { bait(it.loc.coords, bait, leftover) }
            }
        }
        onPlayerSoftTimer(TIMER) { player.trapTick() }
        onPlayerLogout { player.returnTraps() }
    }

    internal fun trapsOf(player: Player): List<Trap> = traps.values.filter { it.owner == player }

    internal fun trapAt(coords: CoordGrid): Trap? = traps[coords]

    internal fun kinds(): List<TrapKind> = kinds

    internal suspend fun ProtectedAccess.lay(kind: TrapKind, ground: Obj?) {
        val level = stat(HUNTER)
        if (level < kind.level) {
            mes("You need a Hunter level of ${kind.level} to set up a ${kind.name}.")
            return
        }
        if (kind.quest.isNotEmpty() && !QuestRequirements.hasCompleted(player, kind.quest)) {
            mes("You need to have completed Eagles' Peak to use a ${kind.name}.")
            return
        }
        if (!limits.canLay(player, areas)) return
        val tile = player.coords
        if (
            ImplingScript.inPuroPuro(tile) ||
                traps.containsKey(tile) ||
                locRepo.findAll(tile).any { it.shape != LocShape.GroundDecor }
        ) {
            mes("You can't set up a trap here.")
            return
        }
        mes("You begin setting up the trap.", ChatType.Spam)
        val laid =
            ground
                ?: run {
                    if (invDel(inv, kind.item).failure) return
                    objRepo.add(kind.item, tile, LAID_OBJ_DURATION, receiver = player, reveal = LAID_OBJ_REVEAL)
                }
        val salted = invDel(inv, ANTI_ODOUR_SALT).success
        anim(LAY_SEQ)
        soundSynth(kind.laySynth)
        delay(LAY_TICKS)
        if (traps.containsKey(tile) || !objRepo.del(laid, Int.MAX_VALUE)) return
        anim(LAY_SEQ)
        val loc = placeLoc(tile, kind.setLoc)
        val trap = Trap(player, kind, tile, loc, TrapState.Set, null, clock.cycle, smoked = salted)
        traps[tile] = trap
        overlay(trap, SET_COLOUR)
        player.softTimer(TIMER, 1)
    }

    private suspend fun ProtectedAccess.dismantle(coords: CoordGrid) {
        val trap = ownTrap(coords) ?: return
        if (trap.state != TrapState.Set && trap.state != TrapState.Broken) return
        if (!inv.hasFreeSpace() && inv.count(trap.kind.item) == 0) {
            mes("You don't have enough inventory space.")
            return
        }
        anim(trap.kind.checkSeq)
        delay(CHECK_TICKS)
        if (traps[coords] !== trap || trap.state == TrapState.Trapping || trap.state == TrapState.Full) return
        if (invAdd(inv, trap.kind.item).failure) return
        removeTrap(trap)
        mes("You dismantle the trap.", ChatType.Spam)
        soundSynth(DISMANTLE_SYNTH)
    }

    private fun ProtectedAccess.investigate(coords: CoordGrid) {
        val trap = ownTrap(coords) ?: return
        mes("This ${trap.kind.name} hasn't caught anything yet.")
    }

    private fun ProtectedAccess.smoke(coords: CoordGrid) {
        val trap = ownTrap(coords) ?: return
        if (trap.kind.smokeRefusal.isNotEmpty()) {
            mes(trap.kind.smokeRefusal)
            return
        }
        if (stat(HUNTER) < SMOKE_LEVEL) {
            mes("You need a Hunter level of $SMOKE_LEVEL to smoke a trap.")
            return
        }
        if (trap.smoked) {
            mes("This trap has already been smoked.")
            return
        }
        anim(BAIT_SEQ)
        trap.smoked = true
        mes("You use the smoke from the torch to remove your scent from the trap.", ChatType.Spam)
    }

    private fun ProtectedAccess.bait(coords: CoordGrid, bait: String, leftover: String?) {
        val trap = ownTrap(coords) ?: return
        if (trap.state != TrapState.Set) return
        if (trap.baited) {
            mes("This trap has already been baited.")
            return
        }
        val used = if (leftover != null) invReplace(inv, bait, 1, leftover) else invDel(inv, bait)
        if (used.failure) return
        anim(BAIT_SEQ)
        trap.baited = true
        mes("You bait the trap.", ChatType.Spam)
    }

    internal suspend fun ProtectedAccess.check(coords: CoordGrid): Boolean {
        val trap = ownTrap(coords) ?: return false
        val creature = trap.creature
        if (trap.state != TrapState.Full || creature == null) return false
        val returned = if (trap.kind.consumed) emptyList() else listOf(trap.kind.item to 1)
        val items = returned + creature.loot.map { it.obj to random.of(it.min, it.max) }
        if (!canAdd(items)) {
            mes("You don't have enough inventory space.")
            return false
        }
        anim(trap.kind.checkSeq)
        delay(CHECK_TICKS)
        if (traps[coords] !== trap || !canAdd(items)) return false
        removeTrap(trap)
        for ((obj, count) in items) invAdd(inv, obj, count)
        statAdvance(HUNTER, creature.xp)
        worldRepo.soundArea(coords, DISMANTLE_SYNTH)
        rumours.onCatch(player, creature.npcs.first())
        mes("You dismantle the trap.", ChatType.Spam)
        val caught = "You've caught ${creature.article} ${creature.name}"
        mes(if (creature.npcs.first() == JERBOA) "$caught... or maybe not." else "$caught.", ChatType.Spam)
        val pet = creature.pet
        if (pet != null) pets.rollSkillingPet(player, pet, HUNTER, creature.petBase)
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
        if (!walkOnto(coords)) return
        lay(trap.kind, null)
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
            if (trap.state == TrapState.Set && age == STEP_OFF_TICKS && coords == trap.coords && !isBusy && !isBusy2) {
                stepOffTile(trap.coords)?.let { walk(it) }
            }
            when (trap.state) {
                TrapState.Set -> {
                    val approach = trap.approach
                    when {
                        approach != null -> approachTick(trap, approach)
                        age > COLLAPSE_TICKS -> collapse(trap)
                        age > 0 && age % ATTEMPT_TICKS == 0 -> attempt(trap)
                    }
                }
                TrapState.Trapping -> if (age >= SETTLE_TICKS) settle(trap)
                TrapState.Failing -> if (age >= trap.kind.failTicks) setState(trap, TrapState.Broken)
                TrapState.Full,
                TrapState.Broken -> if (age >= COLLAPSE_TICKS) collapse(trap)
            }
        }
    }

    private fun attempt(trap: Trap) {
        val npc = nearbyCreature(trap) ?: return
        val creature = trap.kind.creatureFor(npc.id) ?: return
        val approach = Approach(npc, creature, clock.cycle)
        if (trap.kind.snare) {
            npc.teleport(collision, trap.coords)
            npc.anim(creature.approachSeq)
            approach.ready = clock.cycle
        } else {
            val side = sideTile(trap.coords, npc.coords) ?: return
            npc.walkTo(routes, side)
        }
        trap.approach = approach
    }

    private fun Player.approachTick(trap: Trap, approach: Approach) {
        val npc = approach.npc
        if (!npc.isSlotAssigned || !npc.isVisible) {
            trap.approach = null
            return
        }
        val now = clock.cycle
        if (approach.ready == 0) {
            if (npc.coords in cardinal(trap.coords)) {
                npc.faceSquare(trap.coords)
                npc.anim(approach.creature.approachSeq)
                approach.ready = now
            } else if (now - approach.started > APPROACH_TIMEOUT) {
                trap.approach = null
            }
            return
        }
        if (now - approach.ready < 1) return
        trap.approach = null
        resolve(trap, npc, approach.creature)
    }

    private fun Player.resolve(trap: Trap, npc: Npc, creature: Creature) {
        val level = statBase(HUNTER)
        val chance =
            HunterOutfit.applyTo(catchChance256(creature.low, creature.high, level), this) +
                (if (trap.baited) BAIT_BONUS else 0) +
                (if (trap.smoked) SMOKE_BONUS else 0)
        val success =
            playerRepo.findAll(trap.coords).none() && level >= creature.level && random.of(256) < chance
        val synth = if (trap.kind.snare) SNARE_TRAPPING_SYNTH else BOX_TRAPPING_SYNTH
        if (success) {
            val from = npc.coords
            npcRepo.despawn(npc, creature.respawnTicks)
            trap.creature = creature
            trap.trappingLoc = creature.trappingLocFor(trap.coords, from)
            setState(trap, TrapState.Trapping)
            worldRepo.soundArea(trap.coords, synth)
            return
        }
        if (trap.kind.snare) {
            npc.anim(BIRD_LEAVE_SEQ)
            stepOffTile(trap.coords)?.let { npc.walkTo(routes, it) }
        }
        setState(trap, if (trap.kind.failingLoc != null) TrapState.Failing else TrapState.Broken)
        worldRepo.soundArea(trap.coords, synth)
    }

    internal fun catchWithoutLure(trap: Trap, creature: Creature, loc: String) {
        if (trap.state != TrapState.Set) return
        trap.approach = null
        trap.creature = creature
        locRepo.del(trap.loc, Int.MAX_VALUE)
        trap.loc = placeLoc(trap.coords, loc)
        trap.state = TrapState.Full
        trap.stateCycle = clock.cycle
    }

    private fun settle(trap: Trap) {
        setState(trap, TrapState.Full)
        worldRepo.soundArea(trap.coords, if (trap.kind.snare) SNARE_FULL_SYNTH else BOX_FULL_SYNTH)
    }

    private fun nearbyCreature(trap: Trap): Npc? {
        val center = trap.coords
        val busy = traps.values.mapNotNull { it.approach?.npc }.toSet()
        return npcRepo
            .findAll(ZoneKey.from(center), zoneRadius = 1)
            .filter { trap.kind.creatureFor(it.id) != null && it.isSlotAssigned && it.isVisible && it !in busy }
            .filter { it.coords.level == center.level && distance(it.coords, center) <= RANGE }
            .minByOrNull { distance(it.coords, center) }
    }

    private fun sideTile(trap: CoordGrid, from: CoordGrid): CoordGrid? =
        cardinal(trap)
            .filter { !collision.isWalkBlocked(it) && !traps.containsKey(it) }
            .minByOrNull { distance(it, from) }

    private fun setState(trap: Trap, state: TrapState) {
        val internal =
            when (state) {
                TrapState.Set -> trap.kind.setLoc
                TrapState.Trapping -> trap.trappingLoc!!
                TrapState.Full -> trap.creature!!.fullLoc
                TrapState.Failing -> trap.kind.failingLoc!!
                TrapState.Broken -> trap.kind.brokenLoc
            }
        locRepo.del(trap.loc, Int.MAX_VALUE)
        trap.loc = placeLoc(trap.coords, internal)
        trap.state = state
        trap.stateCycle = clock.cycle
        when (state) {
            TrapState.Full -> overlay(trap, FULL_COLOUR)
            TrapState.Broken -> overlay(trap, BROKEN_COLOUR)
            else -> Unit
        }
    }

    private fun overlay(trap: Trap, colour: Int) {
        if (!trap.kind.overlay) return
        val type = ServerCacheManager.getObject(trap.loc.id) ?: return
        ClientScripts.addOverlayTimerLoc(
            player = trap.owner,
            coords = trap.coords,
            loc = type,
            shape = trap.loc.shape,
            timer = OVERLAY_TIMER,
            ticks = OVERLAY_TICKS,
            colour = colour,
            isDespawnTimer = true,
        )
    }

    private fun Player.collapse(trap: Trap) {
        removeTrap(trap)
        objRepo.add(trap.kind.item, trap.coords, OBJ_DURATION, receiver = this, reveal = OBJ_DURATION)
        mes(trap.kind.collapseText, if (trap.kind.snare) ChatType.GameMessage else ChatType.Spam)
    }

    private fun Player.returnTraps() {
        for (trap in trapsOf(this)) {
            removeTrap(trap)
            objRepo.add(trap.kind.item, trap.coords, OBJ_DURATION, receiver = this, reveal = OBJ_DURATION)
        }
    }

    private fun removeTrap(trap: Trap) {
        traps.remove(trap.coords)
        trap.approach = null
        locRepo.del(trap.loc, Int.MAX_VALUE)
    }

    private fun placeLoc(coords: CoordGrid, internal: String): LocInfo =
        locRepo.add(coords, internal, Int.MAX_VALUE, LocAngle.East, LocShape.CentrepieceStraight)

    private fun stepOffTile(from: CoordGrid): CoordGrid? =
        listOf(from.translate(-1, 0), from.translate(1, 0), from.translate(0, -1), from.translate(0, 1))
            .firstOrNull { !collision.isWalkBlocked(it) && !traps.containsKey(it) }

    private fun loadKinds(): List<TrapKind> {
        val creatures = HunterTrapCreaturesRow.all().groupBy { it.trap }
        return HunterTrapsRow.all().mapIndexed { index, row ->
            TrapKind(
                name = row.name,
                itemType = row.item,
                level = row.level,
                setLoc = locName(row.setLoc.id),
                failingLoc = row.failingLoc.id.takeIf { it != row.brokenLoc.id }?.let { locName(it) },
                brokenLoc = locName(row.brokenLoc.id),
                checkSeq = RSCM.getReverseMapping(RSCMType.SEQ, row.checkSeq.id),
                laySynth = row.laySynth,
                collapseText = row.collapseText,
                quest = row.quest,
                consumed = row.consumed,
                smokeRefusal = row.smokeRefusal,
                overlay = row.overlayTimer,
                snare = index == BIRD_SNARE_INDEX,
                creatures =
                    creatures[index].orEmpty().map { c ->
                        Creature(
                            name = c.name,
                            npcs = c.npc.map { RSCM.getReverseMapping(RSCMType.NPC, it.id) },
                            level = c.level,
                            xp = c.xpTenths / 10.0,
                            trappingLocs = c.trappingLocs.map { locName(it.id) },
                            fullLoc = locName(c.fullLoc.id),
                            loot = c.loot.map { Drop(it.t0.internalName, it.t1, it.t2) },
                            low = c.low,
                            high = c.high,
                            respawnTicks = c.respawnTicks,
                            approachSeq = RSCM.getReverseMapping(RSCMType.SEQ, c.approachSeq.id),
                            baits = c.bait.map { it.internalName },
                            baitLeftover = c.baitLeftover?.internalName,
                            pet = c.pet?.internalName,
                            petBase = c.petBase,
                        )
                    },
            )
        }
    }

    private fun locName(id: Int): String = RSCM.getReverseMapping(RSCMType.LOC, id)

    internal enum class TrapState {
        Set,
        Trapping,
        Full,
        Failing,
        Broken,
    }

    internal class Approach(val npc: Npc, val creature: Creature, val started: Int) {
        var ready: Int = 0
    }

    internal class Trap(
        val owner: Player,
        val kind: TrapKind,
        val coords: CoordGrid,
        var loc: LocInfo,
        var state: TrapState,
        var creature: Creature?,
        var stateCycle: Int,
        var smoked: Boolean = false,
        var baited: Boolean = false,
        var approach: Approach? = null,
        var trappingLoc: String? = null,
    )

    internal class Drop(val obj: String, val min: Int, val max: Int = min)

    internal class Creature(
        val name: String,
        val npcs: List<String>,
        val level: Int,
        val xp: Double,
        val trappingLocs: List<String>,
        val fullLoc: String,
        val loot: List<Drop>,
        val low: Int,
        val high: Int,
        val respawnTicks: Int,
        val approachSeq: String,
        val baits: List<String>,
        val baitLeftover: String?,
        val pet: String?,
        val petBase: Int,
    ) {
        val npcIds: Set<Int> by lazy { npcs.map { it.asRSCM(RSCMType.NPC) }.toSet() }

        val checkLocs: List<String>
            get() = if (fullLoc in trappingLocs) trappingLocs else listOf(fullLoc)

        val article: String
            get() = if (name.first() in "aeiou") "an" else "a"

        fun trappingLocFor(trap: CoordGrid, from: CoordGrid): String {
            if (trappingLocs.size < 4) return trappingLocs.first()
            val index =
                when {
                    from.z < trap.z -> NORTH
                    from.x < trap.x -> EAST
                    from.z > trap.z -> SOUTH
                    else -> WEST
                }
            return trappingLocs[index]
        }
    }

    internal class TrapKind(
        val name: String,
        val itemType: ItemServerType,
        val level: Int,
        val setLoc: String,
        val failingLoc: String?,
        val brokenLoc: String,
        val checkSeq: String,
        val laySynth: Int,
        val collapseText: String,
        val quest: String,
        val consumed: Boolean,
        val smokeRefusal: String,
        val overlay: Boolean,
        val snare: Boolean,
        val creatures: List<Creature>,
    ) {
        val item: String = itemType.internalName
        val failTicks: Int = if (snare) 1 else SETTLE_TICKS

        fun creatureFor(npc: Int): Creature? = creatures.firstOrNull { npc in it.npcIds }
    }

    internal companion object {
        const val HUNTER = "stat.hunter"
        const val TIMER = "timer.hunter_traps"
        const val JERBOA = "npc.varlamore_hunterjerboa01"
        const val LAY_SEQ = "seq.human_laytrap"
        const val BAIT_SEQ = "seq.hunting_baiting_small_traps"
        const val BIRD_LEAVE_SEQ = "seq.bird_leavetrap"
        const val ANTI_ODOUR_SALT = "obj.anti_odour_salt"
        const val DISMANTLE_SYNTH = "synth.hunting_trap_dismantle"
        const val BOX_TRAPPING_SYNTH = "synth.hunting_box_trap_trapping"
        const val BOX_FULL_SYNTH = "synth.hunting_box_trap_full"
        const val SNARE_TRAPPING_SYNTH = "synth.hunting_snare_trapping"
        const val SNARE_FULL_SYNTH = "synth.hunting_snare_full"
        val TORCHES = listOf("obj.torch_lit", "obj.wint_torch")
        const val LAY_TICKS = 3
        const val STEP_OFF_TICKS = 2
        const val CHECK_TICKS = 2
        const val ATTEMPT_TICKS = 3
        const val APPROACH_TIMEOUT = 6
        const val SETTLE_TICKS = 2
        const val RANGE = 2
        const val COLLAPSE_TICKS = 100
        const val OBJ_DURATION = 100
        const val LAID_OBJ_DURATION = 300
        const val LAID_OBJ_REVEAL = 100
        const val SMOKE_LEVEL = 39
        const val BAIT_BONUS = 3
        const val SMOKE_BONUS = 2
        const val BIRD_SNARE_INDEX = 0
        const val OVERLAY_TIMER = 4
        const val OVERLAY_TICKS = 101
        const val SET_COLOUR = 0xffd100
        const val FULL_COLOUR = 0x1eff43
        const val BROKEN_COLOUR = 0xff0010

        private const val NORTH = 0
        private const val EAST = 1
        private const val SOUTH = 2
        private const val WEST = 3

        fun cardinal(c: CoordGrid): List<CoordGrid> =
            listOf(c.translate(0, 1), c.translate(1, 0), c.translate(0, -1), c.translate(-1, 0))

        fun distance(a: CoordGrid, b: CoordGrid): Int = max(abs(a.x - b.x), abs(a.z - b.z))

        private val TRAP_LIMITS by lazy { enum<Int, Int>("hunter_trap_limits") }

        fun maxTraps(level: Int): Int = byLevel(TRAP_LIMITS, level)

        fun byLevel(limits: EnumTypeMap<Int, Int>, level: Int): Int =
            limits.getValue(limits.keys.filter { it <= level }.maxOrNull() ?: limits.keys.min())

        fun catchChance256(low: Int, high: Int, level: Int): Int =
            (SkillingSuccessRate.successRate(low, high, level.coerceIn(1, 99), 99) * 256).roundToInt()
    }
}

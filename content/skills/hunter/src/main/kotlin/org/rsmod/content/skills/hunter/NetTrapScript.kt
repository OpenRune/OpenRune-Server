package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.table.hunter.HunterNetTrapsRow
import org.rsmod.content.skills.hunter.HunterTrapScript.Companion.catchChance256
import org.rsmod.content.skills.hunter.HunterTrapScript.Companion.distance
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class NetTrapScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val collision: CollisionFlagMap,
    private val areas: AreaChecker,
    private val random: GameRandom,
    private val clock: MapClock,
    private val rumours: HunterRumours,
    private val limits: HunterTrapLimits,
) : PluginScript() {
    private val traps = HashMap<CoordGrid, NetTrap>()
    private val trees: List<Tree> by lazy { HunterNetTrapsRow.all().map(::Tree) }

    override fun ScriptContext.startup() {
        limits.register { player -> traps.values.count { it.owner == player } }
        onOpHeldU(ROPE, SMALL_NET) { makeNetTrap() }
        for (obj in trees.flatMap { tree -> tree.loot.map { it.first } }.distinct()) {
            onOpHeld5(obj) { release(obj, it.slot) }
        }
        for (tree in trees) {
            onOpLoc1(tree.up) { set(tree, LocInfo(it.loc.layer, it.loc.coords, it.loc.entity)) }
            for (loc in listOf(tree.set, tree.netSet, tree.upReset, tree.netReset)) {
                onOpLoc1(loc) { trapAt(it.loc)?.let { trap -> dismantle(trap) } }
            }
            for (loc in listOf(tree.set, tree.netSet)) {
                onOpLoc2(loc) { trapAt(it.loc)?.let { investigate() } }
                tree.bait?.let { bait -> onOpLocU(loc, bait) { trapAt(it.loc)?.let { trap -> bait(trap, bait) } } }
                if (tree.smokable) {
                    for (torch in HunterTrapScript.TORCHES) {
                        onOpLocU(loc, torch) { trapAt(it.loc)?.let { trap -> smoke(trap) } }
                    }
                }
            }
            for (loc in listOf(tree.upReset, tree.netReset)) {
                onOpLoc2(loc) { trapAt(it.loc)?.let { trap -> reset(trap) } }
            }
            onOpLoc1(tree.full) { trapAt(it.loc)?.let { trap -> check(trap) } }
            onOpLoc2(tree.full) { trapAt(it.loc)?.let { trap -> reset(trap) } }
        }
        onPlayerSoftTimer(TIMER) { player.tick() }
        onPlayerLogout { player.returnTraps() }
    }

    private fun ProtectedAccess.makeNetTrap() {
        if (stat(HUNTER) < NET_TRAP_LEVEL) {
            mes("You need a Hunter level of $NET_TRAP_LEVEL to make a net trap.")
            return
        }
        val made =
            player.invTransaction(inv) {
                val target = select(inv)
                delete(target, ROPE.asRSCM(RSCMType.OBJ), 1)
                delete(target, SMALL_NET.asRSCM(RSCMType.OBJ), 1)
                add(target, NET_TRAP.asRSCM(RSCMType.OBJ), 1)
            }
        if (made.failure) return
        mes("You tie the rope to the small fishing net to make a net trap.", ChatType.Spam)
    }

    private fun ProtectedAccess.release(obj: String, slot: Int) {
        if (invDel(inv, obj, slot = slot).failure) return
        mes("You release the salamander and it darts away.", ChatType.Spam)
    }

    private fun ProtectedAccess.trapAt(loc: BoundLocInfo): NetTrap? {
        val trap = traps.values.firstOrNull { it.covers(loc.coords) } ?: return null
        if (trap.owner != player) {
            mes("This isn't your trap.")
            return null
        }
        return trap
    }

    private suspend fun ProtectedAccess.set(tree: Tree, original: LocInfo) {
        if (stat(HUNTER) < tree.level) {
            mes("You need a Hunter level of ${tree.level} to set up a net trap here.")
            return
        }
        if (NET_TRAP !in inv) {
            mes("You need a net trap to set a trap here.")
            return
        }
        if (traps.containsKey(original.coords) || !limits.canLay(player, areas)) return
        val net = netTile(original)
        if (collision.isWalkBlocked(net)) {
            mes("You can't set up a trap here.")
            return
        }
        val trap = NetTrap(player, tree, original, net)
        traps[original.coords] = trap
        trap.treeLoc = replaceTree(trap, tree.setting)
        delay(1)
        mes("You begin setting up a trap.", ChatType.Spam)
        anim(SET_SEQ)
        worldRepo.locAnim(trap.treeLoc!!, SETTING_LOC_SEQ)
        soundSynth(SET_SYNTH)
        delay(1)
        if (traps[original.coords] !== trap || invDel(inv, NET_TRAP).failure) {
            clear(trap)
            return
        }
        trap.treeLoc = replaceTree(trap, tree.set)
        HunterTrapScript.overlayTimer(player, trap.treeLoc!!, HunterTrapScript.SET_COLOUR)
        trap.netLoc = locRepo.add(net, tree.netSet, Int.MAX_VALUE, original.angle, original.shape)
        enter(trap, State.Set)
        player.softTimer(TIMER, 1)
    }

    private suspend fun ProtectedAccess.dismantle(trap: NetTrap) {
        if (trap.state != State.Set && trap.state != State.Reset) return
        if (inv.freeSpace() < 1 && NET_TRAP !in inv) {
            mes("You don't have enough inventory space.")
            return
        }
        anim(CHECK_SEQ)
        delay(CHECK_TICKS)
        if (traps[trap.tree.coords] !== trap || (trap.state != State.Set && trap.state != State.Reset)) return
        if (invAdd(inv, NET_TRAP).failure) return
        clear(trap)
        mes("You dismantle the trap.", ChatType.Spam)
    }

    private fun ProtectedAccess.investigate() {
        mes("This net trap hasn't caught anything yet.")
    }

    private fun ProtectedAccess.bait(trap: NetTrap, bait: String) {
        if (trap.state != State.Set || trap.baited) return
        if (invDel(inv, bait).failure) return
        anim(HunterTrapScript.BAIT_SEQ)
        trap.baited = true
        mes("You bait the trap.", ChatType.Spam)
    }

    private fun ProtectedAccess.smoke(trap: NetTrap) {
        if (trap.state != State.Set) return
        if (stat(HUNTER) < HunterTrapScript.SMOKE_LEVEL) {
            mes("You need a Hunter level of ${HunterTrapScript.SMOKE_LEVEL} to smoke a trap.")
            return
        }
        if (trap.smoked) {
            mes("This trap has already been smoked.")
            return
        }
        anim(HunterTrapScript.BAIT_SEQ)
        trap.smoked = true
        mes("You use the smoke from the torch to remove your scent from the trap.", ChatType.Spam)
    }

    private suspend fun ProtectedAccess.check(trap: NetTrap): Boolean {
        if (trap.state != State.Full) return false
        val loot = rollLoot(trap.def)
        if (inv.freeSpace() < 1 && NET_TRAP !in inv) {
            mes("You don't have enough inventory space.")
            return false
        }
        anim(CHECK_SEQ)
        delay(CHECK_TICKS)
        if (traps[trap.tree.coords] !== trap || trap.state != State.Full) return false
        if (invAdd(inv, NET_TRAP).failure) return false
        clear(trap)
        invAddOrDrop(objRepo, loot)
        statAdvance(HUNTER, trap.def.xp)
        rumours.onCatch(player, trap.def.npc)
        if (trap.def.npc == TECU) {
            mes("The salamander that pops out of the net doesn't look to be fully grown.", ChatType.Spam)
            mes("You dismantle the trap.", ChatType.Spam)
        } else {
            mes("You dismantle the trap.", ChatType.Spam)
            mes("You've caught ${HunterRumours.article(trap.def.name)} ${trap.def.name}.", ChatType.Spam)
        }
        return true
    }

    private suspend fun ProtectedAccess.reset(trap: NetTrap) {
        when (trap.state) {
            State.Full -> if (!check(trap)) return
            State.Reset -> dismantle(trap)
            else -> return
        }
        if (traps.containsKey(trap.tree.coords) || NET_TRAP !in inv) return
        delay(RELAY_TICKS)
        if (traps.containsKey(trap.tree.coords)) return
        set(trap.def, trap.tree)
    }

    private fun rollLoot(tree: Tree): String {
        val total = tree.loot.sumOf { it.second }
        var roll = random.of(total)
        for ((obj, weight) in tree.loot) {
            if (roll < weight) return obj
            roll -= weight
        }
        return tree.loot.first().first
    }

    private fun stepOffNet(from: CoordGrid): CoordGrid? =
        listOf(from.translate(-1, 0), from.translate(1, 0), from.translate(0, -1), from.translate(0, 1))
            .firstOrNull { tile -> !collision.isWalkBlocked(tile) && traps.values.none { it.net == tile } }

    private fun Player.tick() {
        val own = traps.values.filter { it.owner == this }
        if (own.isEmpty()) {
            clearSoftTimer(TIMER)
            return
        }
        val now = clock.cycle
        for (trap in own) {
            val age = now - trap.stateCycle
            when (trap.state) {
                State.Setting -> {}
                State.Set -> {
                    if (age == STEP_OFF_TICKS && coords == trap.net && !isBusy && !isBusy2) {
                        stepOffNet(trap.net)?.let { walk(it) }
                    }
                    val lured = trap.lured
                    when {
                        lured != null -> if (now - trap.luredCycle >= ROLL_DELAY) resolve(trap, lured)
                        age > HunterTrapScript.COLLAPSE_TICKS -> collapse(trap)
                        age > 0 && age % HunterTrapScript.ATTEMPT_TICKS == 0 -> lure(trap)
                    }
                }
                State.Catching -> if (age >= SETTLE_TICKS) {
                    trap.treeLoc = replaceCaught(trap, trap.def.full)
                    HunterTrapScript.overlayTimer(this, trap.treeLoc!!, HunterTrapScript.FULL_COLOUR, FULL_TIMER_TICKS)
                    enter(trap, State.Full)
                }
                State.Failing -> if (age >= SETTLE_TICKS) {
                    trap.treeLoc = replaceCaught(trap, trap.def.failed)
                    enter(trap, State.Failed)
                }
                State.Failed -> if (age >= FAILED_TICKS) toReset(trap)
                State.Full,
                State.Reset -> if (age >= HunterTrapScript.COLLAPSE_TICKS) collapse(trap)
            }
        }
    }

    private fun lure(trap: NetTrap) {
        val busy = traps.values.mapNotNull { it.lured }.toSet()
        val npcId = trap.def.npc.asRSCM(RSCMType.NPC)
        val npc =
            npcRepo
                .findAll(ZoneKey.from(trap.net), zoneRadius = 1)
                .filter { it.id == npcId && it.isSlotAssigned && it.isVisible && it !in busy }
                .filter { it.coords.level == trap.net.level && distance(it.coords, trap.net) <= LURE_RANGE }
                .minByOrNull { distance(it.coords, trap.net) } ?: return
        npc.teleport(collision, trap.net)
        npc.faceSquare(trap.tree.coords)
        trap.lured = npc
        trap.luredCycle = clock.cycle
    }

    private fun Player.resolve(trap: NetTrap, npc: Npc) {
        trap.lured = null
        if (!npc.isSlotAssigned || !npc.isVisible) return
        val level = statBase(HUNTER)
        val chance =
            HunterOutfit.applyTo(catchChance256(trap.def.low, trap.def.high, level), this) +
                (if (trap.baited) HunterTrapScript.BAIT_BONUS else 0) +
                (if (trap.smoked) HunterTrapScript.SMOKE_BONUS else 0)
        val success = coords != trap.net && level >= trap.def.level && random.of(256) < chance
        removeNet(trap)
        if (success) {
            npcRepo.despawn(npc, RESPAWN_TICKS)
            trap.treeLoc = replaceCaught(trap, trap.def.catching)
            enter(trap, State.Catching)
            worldRepo.soundArea(trap.net, CATCH_SYNTH)
            worldRepo.soundArea(trap.net, SPRING_SYNTH)
        } else {
            trap.treeLoc = replaceCaught(trap, trap.def.failing)
            enter(trap, State.Failing)
            worldRepo.soundArea(trap.net, SPRING_SYNTH)
        }
    }

    private fun toReset(trap: NetTrap) {
        restoreTree(trap)
        trap.treeLoc = replaceTree(trap, trap.def.upReset)
        trap.netLoc = locRepo.add(trap.net, trap.def.netReset, Int.MAX_VALUE, trap.tree.angle, trap.tree.shape)
        enter(trap, State.Reset)
    }

    private fun Player.collapse(trap: NetTrap) {
        clear(trap)
        objRepo.add(NET_TRAP, trap.net, OBJ_DURATION, receiver = this, reveal = OBJ_DURATION)
        mes("The net trap that you set has collapsed.", ChatType.Spam)
    }

    private fun Player.returnTraps() {
        for (trap in traps.values.filter { it.owner == this }) {
            clear(trap)
            if (trap.state != State.Setting) {
                objRepo.add(NET_TRAP, trap.net, OBJ_DURATION, receiver = this, reveal = OBJ_DURATION)
            }
        }
    }

    private fun enter(trap: NetTrap, state: State) {
        trap.state = state
        trap.stateCycle = clock.cycle
    }

    private fun replaceTree(trap: NetTrap, internal: String): LocInfo {
        val tree = trap.tree
        val loc = LocInfo(tree.layer, tree.coords, LocEntity(internal.asRSCM(RSCMType.LOC), tree.shapeId, tree.angleId))
        locRepo.add(loc, Int.MAX_VALUE)
        return loc
    }

    private fun replaceCaught(trap: NetTrap, internal: String): LocInfo {
        val anchor = if (trap.tree.angleId <= 1) trap.tree.coords else trap.net
        val current = trap.treeLoc
        if (anchor != trap.tree.coords && current != null && current.coords == trap.tree.coords) {
            if (current != trap.tree) locRepo.del(current, Int.MAX_VALUE)
            locRepo.del(trap.tree, Int.MAX_VALUE)
        }
        val loc = LocInfo(trap.tree.layer, anchor, LocEntity(internal.asRSCM(RSCMType.LOC), trap.tree.shapeId, trap.tree.angleId))
        locRepo.add(loc, Int.MAX_VALUE)
        return loc
    }

    private fun removeNet(trap: NetTrap) {
        trap.netLoc?.let { locRepo.del(it, Int.MAX_VALUE) }
        trap.netLoc = null
    }

    private fun restoreTree(trap: NetTrap) {
        val current = trap.treeLoc
        if (current != null && current.coords != trap.tree.coords) locRepo.del(current, Int.MAX_VALUE)
        locRepo.add(trap.tree, Int.MAX_VALUE)
        trap.treeLoc = trap.tree
    }

    private fun clear(trap: NetTrap) {
        traps.remove(trap.tree.coords)
        trap.lured = null
        removeNet(trap)
        restoreTree(trap)
    }

    private fun netTile(tree: LocInfo): CoordGrid =
        when (tree.angleId) {
            0 -> tree.coords.translate(0, 1)
            1 -> tree.coords.translate(1, 0)
            2 -> tree.coords.translate(0, -1)
            else -> tree.coords.translate(-1, 0)
        }

    internal enum class State {
        Setting,
        Set,
        Catching,
        Full,
        Failing,
        Failed,
        Reset,
    }

    internal class NetTrap(val owner: Player, val def: Tree, val tree: LocInfo, val net: CoordGrid) {
        var state: State = State.Setting
        var stateCycle: Int = 0
        var treeLoc: LocInfo? = null
        var netLoc: LocInfo? = null
        var lured: Npc? = null
        var luredCycle: Int = 0
        var baited: Boolean = false
        var smoked: Boolean = false

        fun covers(coords: CoordGrid): Boolean = coords == tree.coords || coords == net
    }

    internal class Tree(row: HunterNetTrapsRow) {
        val name: String = row.name
        val npc: String = RSCM.getReverseMapping(RSCMType.NPC, row.npc.id)
        val level: Int = row.level
        val xp: Double = row.xpTenths / 10.0
        private val locs: List<String> = row.locs.map { RSCM.getReverseMapping(RSCMType.LOC, it.id) }
        val up: String = locs[0]
        val setting: String = locs[1]
        val set: String = locs[2]
        val netSet: String = locs[3]
        val catching: String = locs[4]
        val full: String = locs[5]
        val failing: String = locs[6]
        val failed: String = locs[7]
        val upReset: String = locs[8]
        val netReset: String = locs[9]
        val loot: List<Pair<String, Int>> = row.loot.map { it.t0.internalName to it.t1 }
        val low: Int = row.low
        val high: Int = row.high
        val bait: String? = row.bait?.internalName
        val smokable: Boolean = row.smokable
    }

    internal companion object {
        const val HUNTER = "stat.hunter"
        const val TIMER = "timer.hunter_net_traps"
        const val FULL_TIMER_TICKS = 100
        const val STEP_OFF_TICKS = 1
        const val ROPE = "obj.rope"
        const val SMALL_NET = "obj.net"
        const val NET_TRAP = "obj.net_trap"
        const val TECU = "npc.salamander_mountain"
        const val SET_SEQ = "seq.hunting_setting_sapling_trap"
        const val SETTING_LOC_SEQ = "seq.hunting_sapling_setting"
        const val CHECK_SEQ = "seq.human_hunting_dismantle_net"
        const val SET_SYNTH = "synth.hunting_net_trap_set"
        const val CATCH_SYNTH = "synth.salamander_hit"
        const val SPRING_SYNTH = "synth.hunting_net_trap_spring"
        const val NET_TRAP_LEVEL = 29
        const val LURE_RANGE = 2
        const val ROLL_DELAY = 3
        const val SETTLE_TICKS = 2
        const val FAILED_TICKS = 3
        const val CHECK_TICKS = 2
        const val RELAY_TICKS = 4
        const val RESPAWN_TICKS = 1
        const val OBJ_DURATION = 100
    }
}

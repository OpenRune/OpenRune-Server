package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.quest.manager.QuestRequirements
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
class MonkeyTrapScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val routes: RouteFactory,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
    private val clock: MapClock,
    private val rumours: HunterRumours,
) : PluginScript() {
    private val traps = HashMap<CoordGrid, MonkeyTrap>()
    private val mounted = HashSet<Player>()
    private val regrowing = ArrayList<Regrowth>()

    override fun ScriptContext.startup() {
        onOpNpc1(GORILLA) { mount() }
        onOpLoc1(DISMOUNT) { dismount() }
        onOpLoc1(UNSET) { set(it.loc) }
        onOpLoc1(SET) { own(it.loc.coords)?.let { trap -> dismantle(trap) } }
        onOpLoc2(SET) { own(it.loc.coords)?.let { mes("This monkey trap hasn't caught anything yet.") } }
        for (full in FULL) onOpLoc1(full) { own(it.loc.coords)?.let { trap -> check(trap) } }
        onPlayerSoftTimer(TIMER) { player.tick() }
        onPlayerLogout {
            mounted.remove(player)
            traps.values.filter { it.owner == player }.forEach { clear(it) }
        }
    }

    private fun ProtectedAccess.mount() {
        if (GREEGREE !in player.worn) {
            mes("You need to be disguised as one of Kruk's monkeys to ride the gorilla.")
            return
        }
        transmog(PLAYER_GORILLA)
        mounted += player
    }

    private fun ProtectedAccess.dismount() {
        if (!mounted.remove(player)) return
        resetTransmog()
    }

    private fun ProtectedAccess.own(coords: CoordGrid): MonkeyTrap? {
        val trap = traps[coords] ?: return null
        if (trap.owner != player) {
            mes("This isn't your trap.")
            return null
        }
        return trap
    }

    private suspend fun ProtectedAccess.set(bound: BoundLocInfo) {
        val boulder = LocInfo(bound.layer, bound.coords, bound.entity)
        if (traps.containsKey(boulder.coords)) return
        if (statBase(HUNTER) < LEVEL || !QuestRequirements.hasCompleted(player, MM2)) {
            mes("You need a Hunter level of $LEVEL to set a trap for the monkeys.")
            return
        }
        if (player !in mounted) {
            mes("You'll need to be riding one of the gorillas to set this trap.")
            return
        }
        if (traps.values.any { it.owner == player }) {
            mes("You can only have one monkey trap set up at a time.")
            return
        }
        if (BANANA !in inv) {
            mes("You need a banana to bait the monkey trap.")
            return
        }
        val trap = MonkeyTrap(player, boulder, bound.adjustedWidth, bound.adjustedLength)
        traps[boulder.coords] = trap
        replace(trap, SETTING)
        anim(SET_SEQ)
        delay(SET_TICKS)
        if (traps[boulder.coords] !== trap || invDel(inv, BANANA).failure) {
            clear(trap)
            return
        }
        replace(trap, SET)
        mes("You set up the monkey trap.", ChatType.Spam)
        trap.state = State.Set
        trap.stateCycle = clock.cycle
        player.softTimer(TIMER, 1)
    }

    private suspend fun ProtectedAccess.dismantle(trap: MonkeyTrap) {
        if (trap.state != State.Set) return
        replace(trap, UNSETTING)
        anim(DISMANTLE_SEQ)
        delay(SET_TICKS)
        clear(trap)
        mes("You dismantle the trap.", ChatType.Spam)
    }

    private suspend fun ProtectedAccess.check(trap: MonkeyTrap) {
        if (trap.state != State.Full) return
        val tail = if (random.of(PERFECT_TAIL) == 0) PERFECT else DAMAGED
        if (inv.freeSpace() == 0) {
            mes("You don't have enough inventory space.")
            return
        }
        replace(trap, if (trap.mirrored) CHECKING[1] else CHECKING[0])
        anim(CHECK_SEQ)
        delay(SET_TICKS)
        if (traps[trap.boulder.coords] !== trap || invAdd(inv, tail).failure) return
        clear(trap)
        statAdvance(HUNTER, XP)
        rumours.onCatch(player, MONKEY)
        mes("You dismantle the trap.", ChatType.Spam)
        if (tail == PERFECT) {
            mes("You've caught a monkey's tail. Other than the missing monkey, it's in perfect condition!", ChatType.Spam)
        } else {
            mes("You've caught a monkey's tail.", ChatType.Spam)
        }
    }

    private fun Player.tick() {
        val now = clock.cycle
        for (growth in regrowing.toList()) {
            if (now < growth.next) continue
            val npc = growth.npc
            if (!npc.isSlotAssigned) {
                regrowing.remove(growth)
                continue
            }
            growth.stage++
            if (growth.stage >= SANS_TAIL.size) {
                npc.resetTransmog()
                regrowing.remove(growth)
            } else {
                npc.transmog(npcType(SANS_TAIL[growth.stage]), Int.MAX_VALUE)
                growth.next = now + REGROW_TICKS
            }
        }
        val own = traps.values.filter { it.owner == this }
        if (own.isEmpty() && regrowing.isEmpty()) {
            clearSoftTimer(TIMER)
            return
        }
        for (trap in own) {
            val age = now - trap.stateCycle
            when (trap.state) {
                State.Setting -> {}
                State.Set -> {
                    val lure = trap.lure
                    when {
                        lure != null -> approach(trap, lure, now)
                        age > COLLAPSE_TICKS -> {
                            clear(trap)
                            mes("The monkey trap that you constructed has collapsed.")
                        }
                        age >= ATTEMPT_START -> lure(trap)
                    }
                }
                State.Trapping -> if (age >= SETTLE_TICKS) {
                    replace(trap, if (trap.mirrored) FULL[1] else FULL[0])
                    trap.state = State.Full
                    trap.stateCycle = now
                }
                State.Failing -> if (age >= SETTLE_TICKS) clear(trap)
                State.Full -> if (age >= COLLAPSE_TICKS) clear(trap)
            }
        }
    }

    private fun lure(trap: MonkeyTrap) {
        val center = trap.boulder.coords
        val npc =
            npcRepo
                .findAll(ZoneKey.from(center), zoneRadius = 1)
                .filter { it.type.internalName == MONKEY_NAME && it.transmog == null && it.isSlotAssigned && it.isVisible }
                .filter { distance(it.coords, center) <= LURE_RANGE && traps.values.none { t -> t.lure?.npc === it } }
                .minByOrNull { distance(it.coords, center) } ?: return
        val side = sideTiles(trap).filter { !collision.isWalkBlocked(it) }.minByOrNull { distance(it, npc.coords) } ?: return
        trap.lure = Lure(npc, side, clock.cycle)
        npc.walkTo(routes, side)
    }

    private fun Player.approach(trap: MonkeyTrap, lure: Lure, now: Int) {
        val npc = lure.npc
        if (!npc.isSlotAssigned) {
            trap.lure = null
            return
        }
        if (lure.ready == 0) {
            if (npc.coords == lure.side) {
                npc.faceSquare(trap.boulder.coords, trap.width, trap.length)
                npc.anim(EAT_SEQ)
                lure.ready = now
            } else if (now - lure.started > APPROACH_TIMEOUT) {
                trap.lure = null
            }
            return
        }
        if (now - lure.ready < 1) return
        trap.lure = null
        val chance = HunterOutfit.applyTo(catchChance256(LOW, HIGH, statBase(HUNTER)), this)
        trap.stateCycle = now
        if (random.of(256) < chance) {
            trap.mirrored = if (trap.width >= trap.length) lure.side.z < trap.boulder.coords.z else lure.side.x < trap.boulder.coords.x
            replace(trap, if (trap.mirrored) TRAPPING[1] else TRAPPING[0])
            trap.state = State.Trapping
            npc.anim(EAT_AND_PANIC_SEQ)
            npc.transmog(npcType(SANS_TAIL[0]), Int.MAX_VALUE)
            regrowing += Regrowth(npc, now + REGROW_TICKS)
        } else {
            replace(trap, FAILING)
            trap.state = State.Failing
            npc.anim(PANIC_SEQ)
        }
    }

    private fun sideTiles(trap: MonkeyTrap): List<CoordGrid> {
        val c = trap.boulder.coords
        return if (trap.width >= trap.length) {
            (0 until trap.width).flatMap { listOf(c.translate(it, trap.length), c.translate(it, -1)) }
        } else {
            (0 until trap.length).flatMap { listOf(c.translate(trap.width, it), c.translate(-1, it)) }
        }
    }

    private fun npcType(name: String) = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))!!

    private fun replace(trap: MonkeyTrap, internal: String) {
        val b = trap.boulder
        locRepo.add(LocInfo(b.layer, b.coords, LocEntity(internal.asRSCM(RSCMType.LOC), b.shapeId, b.angleId)), Int.MAX_VALUE)
    }

    private fun clear(trap: MonkeyTrap) {
        traps.remove(trap.boulder.coords)
        trap.lure = null
        locRepo.add(trap.boulder, Int.MAX_VALUE)
    }

    internal enum class State {
        Setting,
        Set,
        Trapping,
        Full,
        Failing,
    }

    internal class Lure(val npc: Npc, val side: CoordGrid, val started: Int) {
        var ready: Int = 0
    }

    internal class Regrowth(val npc: Npc, var next: Int) {
        var stage: Int = 0
    }

    internal class MonkeyTrap(val owner: Player, val boulder: LocInfo, val width: Int, val length: Int) {
        var state: State = State.Setting
        var stateCycle: Int = 0
        var lure: Lure? = null
        var mirrored: Boolean = false
    }

    internal companion object {
        const val HUNTER = "stat.hunter"
        const val TIMER = "timer.hunter_monkey_traps"
        const val MM2 = "quest_monkeymadness2"
        const val GORILLA = "npc.mm2_tortured_gorilla_small"
        const val PLAYER_GORILLA = "npc.mm2_player_gorilla"
        const val DISMOUNT = "loc.mm2_lab_dismount"
        const val GREEGREE = "obj.mm2_kruk_greegree"
        const val BANANA = "obj.banana"
        const val MONKEY = "npc.hunting_monkey"
        const val MONKEY_NAME = "hunting_monkey"
        val SANS_TAIL = listOf("npc.hunting_monkey_sans_tail_1", "npc.hunting_monkey_sans_tail_2", "npc.hunting_monkey_sans_tail_3")
        const val UNSET = "loc.hunting_monkeytrap_unset"
        const val SETTING = "loc.hunting_monkeytrap_setting"
        const val UNSETTING = "loc.hunting_monkeytrap_unsetting"
        const val SET = "loc.hunting_monkeytrap_set"
        const val FAILING = "loc.hunting_monkeytrap_failing"
        val TRAPPING = listOf("loc.hunting_monkeytrap_trapping_0", "loc.hunting_monkeytrap_trapping_1")
        val FULL = listOf("loc.hunting_monkeytrap_full_0", "loc.hunting_monkeytrap_full_1")
        val CHECKING = listOf("loc.hunting_monkeytrap_checking_0", "loc.hunting_monkeytrap_checking_1")
        const val SET_SEQ = "seq.kruk_tortured_ape_deadfall"
        const val DISMANTLE_SEQ = "seq.kruk_tortured_ape_deadfall_reverse"
        const val CHECK_SEQ = "seq.kruk_tortured_ape_deadfall_check"
        const val EAT_SEQ = "seq.mm2_hunter_maniacal_monkey_eat"
        const val PANIC_SEQ = "seq.mm2_hunter_maniacal_monkey_panic"
        const val EAT_AND_PANIC_SEQ = "seq.mm2_hunter_maniacal_monkey_eat_and_panic"
        const val PERFECT = "obj.ballista_rope"
        const val DAMAGED = "obj.damaged_ballista_rope"
        const val PERFECT_TAIL = 5000
        const val XP = 1000.0
        const val LEVEL = 60
        const val LOW = -79
        const val HIGH = 227
        const val SET_TICKS = 2
        const val SETTLE_TICKS = 2
        const val ATTEMPT_START = 8
        const val APPROACH_TIMEOUT = 8
        const val LURE_RANGE = 6
        const val COLLAPSE_TICKS = 100
        const val REGROW_TICKS = 50
    }
}

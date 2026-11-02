package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.table.hunter.HunterMossLizardSitesRow
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
class MossLizardScript
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val routes: RouteFactory,
    private val clock: MapClock,
) : PluginScript() {
    private val sites: List<Site> by lazy { HunterMossLizardSitesRow.all().map { Site(it.rocks, it.traps, it.bushes) } }
    private val halfSet = HashMap<Player, CoordGrid>()
    private val runs = HashMap<Player, MutableList<Run>>()

    override fun ScriptContext.startup() {
        onOpLoc1(ROCK) { trap(it.loc.coords) }
        onOpLoc1(BUSH) { rustle(it.loc.coords) }
        onOpHeldU(KNIFE, LIZARD) { cutTail() }
        onPlayerSoftTimer(TIMER) { player.tick() }
        onPlayerLogout {
            halfSet.remove(player)
            runs.remove(player)
        }
    }

    private suspend fun ProtectedAccess.trap(rock: CoordGrid) {
        val site = sites.firstOrNull { rock in it.rocks } ?: return
        if (statBase(HUNTER) < LEVEL) {
            mes("You need a Hunter level of $LEVEL to set a trap for the lizards.")
            return
        }
        if (ROPE !in inv) {
            mes("You need a rope to set a trap for the lizards.")
            return
        }
        faceSquare(rock)
        anim(TRAP_SEQ)
        val first = halfSet[player]
        val index = site.rocks.indexOf(rock)
        val pair = first?.let { site.rocks.indexOf(it) } ?: -1
        if (pair >= 0 && kotlin.math.abs(pair - index) == 1) {
            val trap = minOf(pair, index)
            VarPlayerIntMapSetter.set(player, TRAP_VARBITS[trap], SET)
            soundSynth(FINISH_SYNTH)
            mes("You finish setting up the trap.")
        }
        halfSet[player] = rock
        soundSynth(HALF_SYNTH)
        mes("You set up the first half of the trap.")
    }

    private fun ProtectedAccess.rustle(bush: CoordGrid) {
        val site = sites.firstOrNull { bush in it.bushes } ?: return
        val level = statBase(HUNTER)
        if (level < LEVEL) {
            mes("You need a Hunter level of $LEVEL to spook the lizards.")
            return
        }
        faceSquare(bush)
        anim(TRAP_SEQ)
        soundSynth(RUSTLE_SYNTH)
        val count = lizards(level)
        if (count > 1) mes("Your hunting knowledge allows you to spook more lizards!")
        val exit = site.bushes.first { it != bush }
        val list = runs.getOrPut(player) { mutableListOf() }
        val order = site.traps.indices.toList().let { if (bush == site.bushes[0]) it.reversed() else it }
        for ((i, trap) in order.take(count).withIndex()) {
            list += Run(site, trap, bush, exit, clock.cycle + 1 + i)
        }
        player.softTimer(TIMER, 1)
    }

    private fun Player.tick() {
        val list = runs[this]
        if (list.isNullOrEmpty()) {
            runs.remove(this)
            clearSoftTimer(TIMER)
            return
        }
        val now = clock.cycle
        for (run in list.toList()) {
            val target = run.site.traps[run.trap]
            val lizard = run.lizard
            when {
                lizard == null -> if (now >= run.spawnCycle) {
                    val npc = Npc(MOSS_LIZARD, run.bush)
                    npcRepo.add(npc, LIZARD_LIFE)
                    npc.moveSpeed = MoveSpeed.Run
                    npc.walkTo(routes, target, speed = MoveSpeed.Run)
                    worldRepo.soundArea(run.bush, SPAWN_SYNTH)
                    run.lizard = npc
                }
                !lizard.isSlotAssigned -> list.remove(run)
                run.caughtCycle > 0 -> when (now - run.caughtCycle) {
                    CATCH_TICKS -> {
                        lizard.anim(TRAPPED_SEQ)
                        VarPlayerIntMapSetter.set(this, TRAP_VARBITS[run.trap], ACTIVATED)
                        worldRepo.soundArea(target, CATCH_SYNTH)
                        findTrapLoc(target)?.let { worldRepo.locAnim(it, TRIGGER_SEQ) }
                    }
                    CATCH_TICKS + 1 -> {
                        VarPlayerIntMapSetter.set(this, TRAP_VARBITS[run.trap], 0)
                        objRepo.add(LIZARD, target, LIZARD_OBJ_DURATION, receiver = this)
                        npcRepo.del(lizard, Int.MAX_VALUE)
                        statAdvance(HUNTER, xp(statBase(HUNTER)))
                        list.remove(run)
                    }
                }
                lizard.coords == target && !run.passed ->
                    if (vars[TRAP_VARBITS[run.trap]] == SET) {
                        worldRepo.soundArea(target, TRIGGER_SYNTH, radius = TRIGGER_RADIUS)
                        run.caughtCycle = now
                    } else {
                        run.passed = true
                        lizard.walkTo(routes, run.exit, speed = MoveSpeed.Run)
                    }
                run.passed && lizard.coords == run.exit -> {
                    npcRepo.del(lizard, Int.MAX_VALUE)
                    list.remove(run)
                }
            }
        }
    }

    private fun findTrapLoc(coords: CoordGrid) = locRepo.findAll(coords).firstOrNull { it.id in trapLocIds }

    private val trapLocIds: Set<Int> by lazy { TRAP_LOCS.map { it.asRSCM(RSCMType.LOC) }.toSet() }

    private fun ProtectedAccess.cutTail() {
        if (invReplace(inv, LIZARD, 1, TAIL).failure) return
        anim(CUT_SEQ)
        mes("You cut the tail off the lizard.")
    }

    internal class Site(val rocks: List<CoordGrid>, val traps: List<CoordGrid>, val bushes: List<CoordGrid>)

    internal class Run(val site: Site, val trap: Int, val bush: CoordGrid, val exit: CoordGrid, val spawnCycle: Int) {
        var lizard: Npc? = null
        var caughtCycle: Int = 0
        var passed: Boolean = false
    }

    internal companion object {
        const val HUNTER = "stat.hunter"
        const val TIMER = "timer.hunter_moss_lizards"
        const val ROCK = "loc.pmoon_lizard_rock"
        const val BUSH = "loc.pmoon_lizard_bush"
        val TRAP_LOCS = listOf("loc.pmoon_trap_1", "loc.pmoon_trap_2", "loc.pmoon_trap_3")
        val TRAP_VARBITS = listOf("varbit.pmoon_lizard_trap_set_1", "varbit.pmoon_lizard_trap_set_2", "varbit.pmoon_lizard_trap_set_3")
        const val ROPE = "obj.rope"
        const val KNIFE = "obj.knife"
        const val LIZARD = "obj.raw_lizard"
        const val TAIL = "obj.pmoon_lizard_tail"
        const val MOSS_LIZARD = "npc.moss_lizard"
        const val TRAP_SEQ = "seq.human_pickuptable_walkmerge"
        const val TRAPPED_SEQ = "seq.moss_lizard_trapped"
        const val TRIGGER_SEQ = "seq.pmoon_lizard_trap_trigger"
        const val CUT_SEQ = "seq.human_cutting_knife"
        const val HALF_SYNTH = "synth.moss_lizard_trap_half"
        const val FINISH_SYNTH = "synth.moss_lizard_trap_finish"
        const val RUSTLE_SYNTH = "synth.moss_lizard_rustle"
        const val SPAWN_SYNTH = "synth.moss_lizard_spawn"
        const val TRIGGER_SYNTH = "synth.moss_lizard_trigger"
        const val CATCH_SYNTH = "synth.moss_lizard_caught"
        const val TRIGGER_RADIUS = 8
        const val LEVEL = 20
        const val SET = 1
        const val ACTIVATED = 2
        const val CATCH_TICKS = 2
        const val LIZARD_LIFE = 30
        const val LIZARD_OBJ_DURATION = 100

        fun lizards(level: Int): Int = 1 + listOf(50, 80).count { level >= it }

        fun xp(level: Int): Double = minOf(90.0, level * 0.9)
    }
}

package org.rsmod.content.bosses.moons.neypotzli

import dev.openrune.types.MoveRestrict
import jakarta.inject.Inject
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.abs
import org.rsmod.api.player.hook.NeypotzliHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.bosses.moons.MoonsFights.Companion.set
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The rope trap in the Earthbound Cavern. A rope strung between two neighbouring rocks sets one of
 * three traps; rustling a bush then sends moss lizards running into it. Each trap is a multiloc on
 * the player's own varbit, so a set trap is only ever seen by the player who set it.
 */
class MossLizardTrap
@Inject
constructor(
    private val hooks: Set<@JvmSuppressWildcards NeypotzliHook>,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val worldQueues: WorldQueueList,
) : PluginScript() {
    private val springing: MutableSet<Player> = Collections.newSetFromMap(IdentityHashMap())

    override fun ScriptContext.startup() {
        onOpLoc1("loc.pmoon_lizard_rock") { tieRope(it.loc.coords) }
        onOpLoc1("loc.pmoon_lizard_bush") { rustle(it.loc.coords) }
        onPlayerLogin {
            for (varbit in TRAP_VARBITS) {
                if (player.vars[varbit] != 0) player.set(varbit, 0)
            }
        }
        onPlayerLogout { springing -= player }
    }

    private suspend fun ProtectedAccess.tieRope(rock: CoordGrid) {
        val site = SITES.firstOrNull { rock in it.rocks } ?: return
        if (!mayTrap()) return
        if (inv.count("obj.rope") == 0) {
            mes("You need some rope to set a trap here.")
            return
        }
        if (TRAP_VARBITS.any { vars[it] != 0 }) {
            mes("You already have a trap set up.")
            return
        }
        val index = SITES.indexOf(site) * ROCKS_PER_SITE + site.rocks.indexOf(rock)
        val first = vars[FIRST_ROCK] - 1
        val neighbours =
            first >= 0 && first / ROCKS_PER_SITE == index / ROCKS_PER_SITE && abs(first - index) == 1
        anim("seq.human_pickupfloor")
        if (!neighbours) {
            player.set(FIRST_ROCK, index + 1)
            soundSynth("synth.bane_attach_rope")
            mes("You set up the first half of the trap.")
            return
        }
        player.set(FIRST_ROCK, 0)
        player.set(TRAP_VARBITS[minOf(first, index) % ROCKS_PER_SITE], TRAP_SET)
        soundSynth("synth.varlamore_pm_lizard_trap_place_01")
        mes("You finish setting up the trap.")
    }

    private suspend fun ProtectedAccess.rustle(bush: CoordGrid) {
        val site = SITES.firstOrNull { bush in it.bushes } ?: return
        if (!mayTrap() || player in springing) return
        val trap = TRAP_VARBITS.indexOfFirst { vars[it] == TRAP_SET }
        if (trap == -1) {
            mes("You should set up a trap before you scare the lizards out.")
            return
        }
        springing += player
        soundSynth("synth.varlamore_pm_rustle_bush_01")
        val pack = if (statRandom("stat.hunter", PACK_LOW, PACK_HIGH, 0)) FULL_PACK else 1
        if (pack > 1) mes("Your hunting knowledge allows you to spook more lizards!")

        val hunter = player
        val snare = site.traps[trap]
        val lizards = mutableListOf<Npc>()
        repeat(pack) { nth ->
            worldQueues.add(SPOOK_DELAY + nth) { lizards += spook(bush, snare) }
        }
        worldQueues.add(SPRING_DELAY) { spring(hunter, trap, snare, lizards) }
    }

    private suspend fun ProtectedAccess.mayTrap(): Boolean {
        if (!neypotzliOpen(hooks)) return false
        if (statBase("stat.hunter") < HUNTER_LEVEL) {
            mes("You need a Hunter level of $HUNTER_LEVEL to trap moss lizards.")
            return false
        }
        return true
    }

    private fun spook(bush: CoordGrid, snare: CoordGrid): Npc {
        val lizard = Npc("npc.moss_lizard", bush)
        npcRepo.add(lizard, LIZARD_LIFESPAN)
        lizard.noneMode()
        lizard.moveRestrict = MoveRestrict.PassThru
        lizard.walk(snare)
        return lizard
    }

    private fun spring(hunter: Player, trap: Int, snare: CoordGrid, lizards: List<Npc>) {
        val caught = lizards.firstOrNull()
        for (lizard in lizards) {
            if (lizard !== caught && lizard.isSlotAssigned) npcRepo.del(lizard, Int.MAX_VALUE)
        }
        worldRepo.soundArea(snare, "synth.varlamore_pm_lizard_trap_close_01")
        worldRepo.soundArea(snare, "synth.lizard_death")
        caught?.anim("seq.moss_lizard_trapped")
        if (hunter in springing) hunter.set(TRAP_VARBITS[trap], TRAP_SPRUNG)

        worldQueues.add(1) {
            if (caught != null && caught.isSlotAssigned) npcRepo.del(caught, Int.MAX_VALUE)
            if (!springing.remove(hunter)) return@add
            hunter.set(TRAP_VARBITS[trap], 0)
            objRepo.add("obj.raw_lizard", snare, CATCH_LIFESPAN, hunter, reveal = CATCH_LIFESPAN)
            val xp = (hunter.statBase("stat.hunter") * XP_PER_LEVEL).coerceAtMost(MAX_XP)
            hunter.statAdvance("stat.hunter", xp)
        }
    }

    private class TrapSite(
        val rocks: List<CoordGrid>,
        val traps: List<CoordGrid>,
        val bushes: List<CoordGrid>,
    )

    private companion object {
        private const val FIRST_ROCK = "varp.pmoon_trap_first_rock"
        private const val ROCKS_PER_SITE = 4
        private const val TRAP_SET = 1
        private const val TRAP_SPRUNG = 2
        private const val HUNTER_LEVEL = 20
        private const val FULL_PACK = 3
        private const val SPOOK_DELAY = 2
        private const val SPRING_DELAY = 8
        private const val LIZARD_LIFESPAN = 50
        private const val CATCH_LIFESPAN = 200
        private const val XP_PER_LEVEL = 0.9
        private const val MAX_XP = 90.0

        // UNVERIFIED: how often a rustle sends out the full pack rather than one lizard.
        private const val PACK_LOW = 26
        private const val PACK_HIGH = 200

        private val TRAP_VARBITS =
            listOf(
                "varbit.pmoon_lizard_trap_set_1",
                "varbit.pmoon_lizard_trap_set_2",
                "varbit.pmoon_lizard_trap_set_3",
            )

        /** Trap `n` hangs between rocks `n` and `n + 1` of its site. */
        private val SITES =
            listOf(
                TrapSite(
                    rocks =
                        listOf(
                            CoordGrid(1377, 9682),
                            CoordGrid(1379, 9680),
                            CoordGrid(1381, 9680),
                            CoordGrid(1383, 9678),
                        ),
                    traps =
                        listOf(CoordGrid(1378, 9681), CoordGrid(1380, 9680), CoordGrid(1382, 9679)),
                    bushes = listOf(CoordGrid(1380, 9676), CoordGrid(1380, 9684)),
                ),
                TrapSite(
                    rocks =
                        listOf(
                            CoordGrid(1388, 9709),
                            CoordGrid(1390, 9707),
                            CoordGrid(1392, 9707),
                            CoordGrid(1394, 9705),
                        ),
                    traps =
                        listOf(CoordGrid(1389, 9708), CoordGrid(1391, 9707), CoordGrid(1393, 9706)),
                    bushes = listOf(CoordGrid(1391, 9703), CoordGrid(1391, 9711)),
                ),
            )
    }
}

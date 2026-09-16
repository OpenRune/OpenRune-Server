package org.rsmod.content.areas.misc.motherlode.circuit

import dev.openrune.types.MoveRestrict
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.random.Random
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.areas.misc.motherlode.MotherlodeMine
import org.rsmod.content.areas.misc.motherlode.PayDirtOre
import org.rsmod.content.areas.misc.motherlode.cleaningPayDirtTotal
import org.rsmod.content.areas.misc.motherlode.sackCapacity
import org.rsmod.content.areas.misc.motherlode.sackCount
import org.rsmod.content.areas.misc.motherlode.sackTotal
import org.rsmod.content.areas.misc.motherlode.setSackCount
import org.rsmod.content.areas.misc.motherlode.syncMotherlodeVars
import org.rsmod.content.areas.misc.motherlode.takeCleanedPayDirt
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * World-wide state of Percy's washing machine: the two water wheel struts that periodically break,
 * the pay-dirt each player has deposited, and the cosmetic pay-dirt that floats down the channel.
 * Deposited pay-dirt only advances while at least one strut is intact.
 */
@Singleton
class MotherlodeWaterCircuit
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val collision: CollisionFlagMap,
    private val mapClock: MapClock,
    private val xpMods: XpModifiers,
) {
    private val struts =
        listOf(
            Strut(CoordGrid(3742, 5663, 0), CoordGrid(3743, 5662, 0), CoordGrid(3743, 5665, 0)),
            Strut(CoordGrid(3742, 5669, 0), CoordGrid(3743, 5668, 0), CoordGrid(3743, 5671, 0)),
        )
    private val batches = HashMap<Player, ArrayDeque<CleaningBatch>>()
    private val floatingPayDirt = ArrayList<Npc>()
    private var initialised = false

    val isFlowing: Boolean
        get() = struts.any { !it.broken }

    fun isBrokenStrut(coords: CoordGrid): Boolean = struts.any { it.strut == coords && it.broken }

    fun repairStrut(coords: CoordGrid): Boolean {
        val strut = struts.firstOrNull { it.strut == coords && it.broken } ?: return false
        repair(strut)
        return true
    }

    fun deposit(player: Player, count: Int) {
        batches.getOrPut(player) { ArrayDeque() }.addLast(CleaningBatch(count, CLEANING_CYCLES))
        spawnFloatingPayDirt()
    }

    fun resume(player: Player) {
        val pending = player.cleaningPayDirtTotal
        if (pending > 0 && player !in batches) {
            batches[player] = ArrayDeque(listOf(CleaningBatch(pending, CLEANING_CYCLES)))
        }
    }

    fun remove(player: Player) {
        batches.remove(player)
    }

    fun tick() {
        if (!initialised) {
            struts.forEach(::repair)
            initialised = true
        }
        for (strut in struts) {
            if (!strut.broken && mapClock.cycle >= strut.breakCycle) {
                breakStrut(strut)
            }
        }
        if (!isFlowing) {
            return
        }
        advanceFloatingPayDirt()
        advanceBatches()
    }

    private fun advanceBatches() {
        val iterator = batches.entries.iterator()
        while (iterator.hasNext()) {
            val (player, queue) = iterator.next()
            for (batch in queue) {
                batch.remainingCycles--
            }
            while (queue.isNotEmpty() && queue.first().remainingCycles <= 0) {
                clean(player, queue.removeFirst().count)
            }
            if (queue.isEmpty()) {
                iterator.remove()
            }
        }
    }

    private fun clean(player: Player, count: Int) {
        val washed = player.takeCleanedPayDirt(count)
        if (washed.isEmpty()) {
            return
        }
        var nuggets = 0
        var xp = 0.0
        for (ore in washed) {
            if (ore == PayDirtOre.Nugget) {
                if (++nuggets > MAX_NUGGETS_PER_BATCH) {
                    continue
                }
            }
            player.setSackCount(ore, player.sackCount(ore) + 1)
            xp += ore.xp
        }
        if (xp > 0.0) {
            player.statAdvance("stat.mining", xp * xpMods.get(player, "stat.mining"))
        }
        player.syncMotherlodeVars()
        if (player.sackTotal >= player.sackCapacity) {
            player.mes("Some ore is ready to be collected from the sack. It's getting full.")
        } else {
            player.mes("Some ore is ready to be collected from the sack.")
        }
    }

    private fun breakStrut(strut: Strut) {
        strut.broken = true
        locRepo.add(strut.strut, "loc.motherlode_wheel_strut_broken", Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
        locRepo.add(strut.wheel, "loc.motherlode_wheel_broken", Int.MAX_VALUE, LocAngle.East, LocShape.CentrepieceStraight)
        strut.foamLoc?.let { locRepo.del(it, Int.MAX_VALUE) }
        strut.foamLoc = null
    }

    private fun repair(strut: Strut) {
        strut.broken = false
        strut.breakCycle = mapClock.cycle + STRUT_ROUND_CYCLES * Random.nextInt(2, 5)
        locRepo.add(strut.strut, "loc.motherlode_wheel_strut_fixed", Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
        locRepo.add(strut.wheel, "loc.motherlode_wheel_fixed", Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
        strut.foamLoc =
            locRepo.add(strut.foam, "loc.waterfall_foam", Int.MAX_VALUE, LocAngle.North, LocShape.CentrepieceStraight)
    }

    private fun spawnFloatingPayDirt() {
        val npc = Npc("npc.motherlode_paydirt", MotherlodeMine.PAYDIRT_CHANNEL_START)
        npc.mode = NpcMode.None
        npc.moveRestrict = MoveRestrict.PassThru
        npcRepo.add(npc, Int.MAX_VALUE)
        floatingPayDirt += npc
    }

    private fun advanceFloatingPayDirt() {
        val iterator = floatingPayDirt.iterator()
        while (iterator.hasNext()) {
            val npc = iterator.next()
            if (npc.coords == MotherlodeMine.PAYDIRT_CHANNEL_END) {
                npcRepo.del(npc, Int.MAX_VALUE)
                iterator.remove()
                continue
            }
            npc.teleport(collision, npc.coords.translateZ(-1))
        }
    }

    private class Strut(val strut: CoordGrid, val wheel: CoordGrid, val foam: CoordGrid) {
        var broken: Boolean = false
        var breakCycle: Int = 0
        var foamLoc: LocInfo? = null
    }

    private class CleaningBatch(val count: Int, var remainingCycles: Int)

    private companion object {
        const val CLEANING_CYCLES = 13
        const val STRUT_ROUND_CYCLES = 97
        const val MAX_NUGGETS_PER_BATCH = 3
    }
}

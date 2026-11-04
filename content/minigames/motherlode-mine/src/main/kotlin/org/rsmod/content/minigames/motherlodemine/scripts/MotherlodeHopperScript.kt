package org.rsmod.content.minigames.motherlodemine.scripts

import jakarta.inject.Inject
import org.rsmod.api.delivery.DeliveryLineRegistry
import org.rsmod.api.delivery.DeliveryManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.content.minigames.motherlodemine.MotherlodeMachine
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants.isMotherlodeUpperFloor
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeDelivery
import org.rsmod.content.minigames.motherlodemine.configs.motherlodeSuperHopperUnlocked
import org.rsmod.game.MapClock
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// Ore is rolled at deposit time so the persistent delivery payload is stable across reconnects.
class MotherlodeHopperScript
@Inject
constructor(
    private val deliveries: DeliveryManager,
    private val registry: DeliveryLineRegistry,
    private val machine: MotherlodeMachine,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        MotherlodeDelivery.waterStopped = { !machine.flowedThisTick }
        MotherlodeDelivery.currentCycle = { mapClock.cycle }
        registry.register(MotherlodeDelivery.PAY_DIRT_LINE)
        onOpLoc1(MotherlodeConstants.HOPPER_LOC) { deposit(it.loc) }
        // The upper hopper's blocked boundary requires an adjacent approach operation.
        onApLoc1(MotherlodeConstants.HOPPER_LOC) {
            if (!it.loc.coords.isMotherlodeUpperFloor()) {
                apRange(-1)
            } else if (isWithinApRange(it.loc, distance = 1)) {
                deposit(it.loc)
            }
        }
    }

    private suspend fun ProtectedAccess.deposit(hopper: BoundLocInfo) {
        arriveDelay()

        if (hopper.coords.isMotherlodeUpperFloor() && !player.motherlodeSuperHopperUnlocked) {
            mes("You need to buy access to the super hopper from Prospector Percy first.")
            return
        }

        val line = MotherlodeDelivery.PAY_DIRT_LINE
        val available = inv.count(MotherlodeConstants.OBJ_PAYDIRT)
        if (available <= 0) {
            mes(MotherlodeConstants.HOPPER_NOTHING_MESSAGE)
            return
        }

        if (deliveries.pendingCount(player, line) > 0) {
            mes(MotherlodeConstants.HOPPER_BUSY_MESSAGE)
            return
        }

        if (deliveries.committedCount(player, line) >= deliveries.capacity(player, line)) {
            mes(MotherlodeConstants.SACK_FULL_MESSAGE)
            return
        }

        val count = minOf(available, deliveries.capacity(player, line) - deliveries.committedCount(player, line))

        if (invDel(inv, MotherlodeConstants.OBJ_PAYDIRT, count).failure) {
            return
        }

        player.faceSquare(hopper.coords)
        anim("seq.human_pickuptable")
        mes("You deposit the pay-dirt into the hopper.")

        val payloads = rollPayloads(count)
        deliveries.enqueueAll(player, line, payloads)

        machine.spawnPayDirtFloater()

        if (!machine.isWaterFlowing) {
            mes(MotherlodeConstants.MACHINE_BROKEN_MESSAGE)
        }

        soundSynth("synth.watersplash")
    }

    private fun ProtectedAccess.rollPayloads(count: Int): List<Int> {
        val payloads = ArrayList<Int>(count)
        var nuggetsRolled = 0
        repeat(count) {
            if (nuggetsRolled < MotherlodeConstants.MAX_GOLDEN_NUGGETS_PER_DEPOSIT &&
                random.of(MotherlodeConstants.NUGGET_DENOMINATOR) < MotherlodeConstants.NUGGET_NUMERATOR) {
                nuggetsRolled++
                payloads += MotherlodeConstants.payloadIdOf.getValue(MotherlodeConstants.OBJ_NUGGET)
                return@repeat
            }
            payloads += MotherlodeConstants.payloadIdOf.getValue(rollOreTier())
        }
        return payloads
    }

    private fun ProtectedAccess.rollOreTier(): String {
        for (tier in MotherlodeConstants.ORE_TIERS) {
            if (player.miningLvl < tier.req) {
                continue
            }
            if (statRandom("stat.mining", tier.low, tier.high, invisibleLvls)) {
                return tier.obj
            }
        }
        // Coal is an unconditional fallback; its published chart is a display artifact.
        return MotherlodeConstants.OBJ_COAL
    }
}

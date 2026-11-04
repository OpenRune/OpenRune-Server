package org.rsmod.content.minigames.motherlodemine.scripts

import jakarta.inject.Inject
import org.rsmod.api.delivery.DeliveryLineRegistry
import org.rsmod.api.delivery.DeliveryManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.minigames.motherlodemine.MotherlodeMachine
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants.isMotherlodeUpperFloor
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeDelivery
import org.rsmod.content.minigames.motherlodemine.configs.motherlodeSuperHopperUnlocked
import org.rsmod.content.minigames.motherlodemine.scripts.MercyScript.Companion.mercyWarnsAboutHopper
import org.rsmod.game.MapClock
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeHopperScript
@Inject
constructor(
    private val deliveries: DeliveryManager,
    private val registry: DeliveryLineRegistry,
    private val machine: MotherlodeMachine,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        MotherlodeDelivery.waterStopped = { !machine.flowedThisTick }
        MotherlodeDelivery.currentCycle = { mapClock.cycle }
        MotherlodeDelivery.xpModifiers = xpMods
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
            mercyWarnsAboutHopper()
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
        val nuggetId = MotherlodeConstants.payloadIdOf.getValue(MotherlodeConstants.OBJ_NUGGET)
        val payloads = ArrayList<Int>(count)
        var nuggets = 0

        for (slot in inv.indices) {
            if (payloads.size >= count) {
                break
            }
            val obj = inv[slot] ?: continue
            if (!obj.isType(MotherlodeConstants.OBJ_PAYDIRT)) {
                continue
            }
            if (invDel(inv, MotherlodeConstants.OBJ_PAYDIRT, 1, slot = slot).failure) {
                continue
            }
            val payload =
                MotherlodeConstants.payloadIdForIndex(obj.vars and MotherlodeConstants.PAYDIRT_ORE_MASK)
                    ?: MotherlodeConstants.payloadIdOf.getValue(rollOreTier())
            if (payload == nuggetId) {
                if (nuggets >= MotherlodeConstants.MAX_GOLDEN_NUGGETS_PER_DEPOSIT) {
                    continue
                }
                nuggets++
            }
            payloads += payload
        }

        if (payloads.isEmpty()) {
            return
        }

        player.faceSquare(hopper.coords)
        anim("seq.human_pickuptable")
        mes("You deposit the pay-dirt into the hopper.")

        deliveries.enqueueAll(player, line, payloads)

        machine.spawnPayDirtFloater()

        if (!machine.isWaterFlowing) {
            mes(MotherlodeConstants.MACHINE_BROKEN_MESSAGE)
        }

        soundSynth("synth.watersplash")
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

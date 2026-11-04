package org.rsmod.content.minigames.motherlodemine.scripts

import jakarta.inject.Inject
import org.rsmod.api.delivery.DeliveryManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeDelivery
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeDelivery.sackTransmit
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeSackScript @Inject constructor(private val deliveries: DeliveryManager) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(MotherlodeConstants.SACK_EMPTY_LOC) { search() }
        onOpLoc1(MotherlodeConstants.SACK_FULL_LOC) { search() }
    }

    private suspend fun ProtectedAccess.search() {
        arriveDelay()

        val line = MotherlodeDelivery.PAY_DIRT_LINE
        val snapshot = deliveries.storedSnapshot(player, line)
        if (snapshot.isEmpty()) {
            mes(MotherlodeConstants.SACK_EMPTY_MESSAGE)
            return
        }

        var takenAny = false
        outer@ for (payload in MotherlodeConstants.sackTierOrderIds) {
            var remaining = snapshot[payload] ?: continue
            val obj = MotherlodeConstants.payloadObjOf[payload] ?: continue
            // Reserve free slots before debiting unstackable ores; only nuggets can share a slot.
            val stackable = obj == MotherlodeConstants.OBJ_NUGGET
            while (remaining > 0) {
                val cap = if (stackable && (inv.count(obj) > 0 || inv.freeSpace() > 0)) {
                    remaining
                } else {
                    minOf(remaining, inv.freeSpace())
                }
                if (cap <= 0) {
                    break@outer
                }
                val batch = random.of(1, cap)
                if (invAdd(inv, obj, batch).failure) {
                    break@outer
                }
                val taken = deliveries.takeStored(player, line, payload, batch)
                if (taken < batch) invDel(inv, obj, batch - taken)
                if (taken <= 0) break
                takenAny = true
                remaining -= taken
            }
        }

        syncTransmit()

        if (!takenAny) {
            mes("You don't have enough space to take anything from the sack.")
            return
        }

        soundSynth("synth.pick2")
        val nowEmpty = deliveries.storedCount(player, line) <= 0
        val message =
            if (nowEmpty) {
                MotherlodeConstants.SACK_COLLECT_EMPTY_MESSAGE
            } else {
                MotherlodeConstants.SACK_COLLECT_MESSAGE
            }
        mes(message)
    }

    private fun ProtectedAccess.syncTransmit() {
        val total = deliveries.storedCount(player, MotherlodeDelivery.PAY_DIRT_LINE)
        player.sackTransmit = total.coerceIn(0, 255)
    }
}

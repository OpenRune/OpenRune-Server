package org.rsmod.content.skills.farming

import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.generic.locs.water.WateringCans
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingSaplingsScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (chain in FarmingSaplings.chains) {
            onOpHeldU(chain.seed.internalName, "obj.plantpot_compost") {
                when {
                    !inv.contains("obj.gardening_trowel") -> mes("You need a gardening trowel to plant a seed in a pot.")
                    player.farmingLvl < chain.level -> mes("You need a Farming level of ${chain.level} to plant this seed.")
                    else -> FarmingSaplings.sow(player, chain, it.firstSlot, it.secondSlot)
                }
            }
            for (can in WateringCans.CANS) {
                onOpHeldU(chain.dry.internalName, can) {
                    FarmingSaplings.water(player, it.firstSlot, can, it.secondSlot)
                }
            }
        }
        for (can in WateringCans.CANS.filter { WateringCans.doses(it)?.let { it < 8 } == true }) {
            for (water in WateringCans.WATER_VESSELS) {
                onOpHeldU(can, water) {
                    WateringCans.refillFrom(player, can, it.firstSlot, water, it.secondSlot)
                }
            }
        }
        onPlayerLogin {
            val millis = System.currentTimeMillis()
            FarmingSaplings.grow(player, millis)
            player.softTimer(TIMER, FarmingClock.ticksUntilNext(player, 5, millis))
        }
        onPlayerSoftTimer(TIMER) {
            val millis = System.currentTimeMillis()
            FarmingSaplings.grow(player, millis)
            player.softTimer(TIMER, FarmingClock.ticksUntilNext(player, 5, millis))
        }
    }

    private companion object {
        const val TIMER = "timer.farming_saplings"
    }
}

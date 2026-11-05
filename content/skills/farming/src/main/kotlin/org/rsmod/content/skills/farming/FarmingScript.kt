package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.generic.locs.water.WateringCans
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Patch ops are bound on every parent loc. The clicked option is read from the child the player
 * currently sees, so one handler covers every patch kind's op layout.
 */
class FarmingScript @Inject constructor(
    private val farming: FarmingService,
    private val compost: FarmingCompostService,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (patch in FarmingPatches.all) {
            for (loc in patch.locs) {
                onOpLoc1(loc) { op(patch, loc, 1, it.loc) }
                onOpLoc2(loc) { op(patch, loc, 2, it.loc) }
                onOpLoc3(loc) { op(patch, loc, 3, it.loc) }
                onOpLoc4(loc) { op(patch, loc, 4, it.loc) }
                onOpLoc5(loc) { op(patch, loc, 5, it.loc) }
                onOpLocU(loc) { useItem(patch, it.objType.internalName, it.invSlot, it.loc) }
            }
        }

        onPlayerLogin {
            farming.refresh(player)
            player.softTimer(TIMER_TRANSMIT, TRANSMIT_INTERVAL)
        }

        onPlayerSoftTimer(TIMER_TRANSMIT) { farming.refresh(player) }
    }

    private suspend fun ProtectedAccess.op(patch: PatchDef, loc: String, op: Int, bound: BoundLocInfo) {
        val state = farming.state(player, patch)
        val parent = ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC))
        val shown = parent?.multiLoc?.getOrNull(state.transmit(state.crop))
        val action = shown?.let(ServerCacheManager::getObject)?.actions?.getOpOrNull(op - 1)?.lowercase()
        with(farming) {
            when {
                action == "rake" -> rake(patch, bound)
                action == "inspect" -> inspect(patch)
                action == "cure" -> plantCure(patch)
                action == "clear" -> if (!dig(patch)) mes(NOTHING)
                action in HARVEST_OPS || action?.startsWith("pick") == true ->
                    if (!harvest(patch, bound)) mes(NOTHING)
                else -> mes(NOTHING)
            }
        }
    }

    internal suspend fun ProtectedAccess.useItem(patch: PatchDef, obj: String, slot: Int, bound: BoundLocInfo) {
        if (compost.handles(obj)) {
            with(compost) { apply(patch, obj, slot) }
            return
        }
        with(farming) {
            when {
                obj == SPADE -> if (!dig(patch)) mes(NOTHING)
                obj == RAKE -> rake(patch, bound)
                obj == PLANT_CURE -> plantCure(patch)
                WateringCans.isCan(obj) -> water(patch, obj, slot)
                FarmingCrops.bySeed(obj) != null -> plant(patch, obj)
                else -> mes(NOTHING)
            }
        }
    }

    private companion object {
        const val TIMER_TRANSMIT = "timer.farming_transmit"
        const val TRANSMIT_INTERVAL = 10
        val HARVEST_OPS = setOf("harvest", "collect")
    }
}

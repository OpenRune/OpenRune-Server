package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.generic.locs.water.WateringCans
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Patch ops are bound on every parent loc. The clicked option is read from the child the player
 * currently sees, so one handler covers every patch kind's op layout.
 */
class FarmingScript @Inject constructor(
    private val farming: FarmingService,
    private val compost: FarmingCompostService,
    private val scarecrow: FarmingScarecrow,
    private val spiritTrees: FarmingSpiritTrees,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (patch in FarmingPatches.all) {
            for (loc in patch.locs) {
                onOpLoc1(loc) { op(patch, loc, 1) }
                onOpLoc2(loc) { op(patch, loc, 2) }
                onOpLoc3(loc) { op(patch, loc, 3) }
                onOpLoc4(loc) { op(patch, loc, 4) }
                onOpLoc5(loc) { op(patch, loc, 5) }
                onOpLocU(loc) { useItem(patch, it.objType.internalName, it.invSlot) }
            }
        }

        onPlayerLogin {
            farming.refresh(player)
            player.softTimer(TIMER_TRANSMIT, TRANSMIT_INTERVAL)
        }

        onPlayerSoftTimer(TIMER_TRANSMIT) { farming.refresh(player) }
    }

    private suspend fun ProtectedAccess.op(patch: PatchDef, loc: String, op: Int) {
        val state = farming.state(player, patch)
        val parent = ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC))
        val shown = parent?.multiLoc?.getOrNull(state.transmit(state.crop, patch.kind))
        val action = shown?.let(ServerCacheManager::getObject)?.actions?.getOpOrNull(op - 1)?.lowercase()
        if (patch.kind == PatchKind.SPIRIT_TREE && action in SPIRIT_OPS) {
            spiritTrees.interact(this, patch, checkNotNull(action))
            return
        }
        if (action == "remove" && state.scarecrow) {
            with(scarecrow) { remove(patch) }
            return
        }
        with(farming) {
            when {
                action == "rake" -> rake(patch)
                action == "inspect" -> inspect(patch)
                action == "cure" -> plantCure(patch)
                action == "check-health" -> checkHealth(patch)
                action == "clear" -> if (!dig(patch)) mes(NOTHING)
                action in HARVEST_OPS || action?.startsWith("pick") == true ->
                    if (!harvest(patch)) mes(NOTHING)
                else -> mes(NOTHING)
            }
        }
    }

    internal suspend fun ProtectedAccess.useItem(patch: PatchDef, obj: String, slot: Int) {
        if (patch.kind == PatchKind.GRAPE_VINE &&
            (obj == FarmingGrapes.SALTPETRE || obj == FarmingGrapes.FERTILISER)
        ) {
            if (!FarmingGrapes.prepareSoil(player, farming, patch, obj, slot)) mes(NOTHING)
            return
        }
        if (compost.handles(obj)) {
            with(compost) { apply(patch, obj, slot) }
            return
        }
        if (obj == FarmingScarecrow.SCARECROW || (obj == SPADE && farming.state(player, patch).scarecrow)) {
            with(scarecrow) {
                if (obj == SPADE) remove(patch) else place(patch)
            }
            return
        }
        if (obj in FarmingBags.seedBox.variants) {
            val seed = FarmingBags.firstSuitableSeed(player) {
                FarmingCrops.bySeed(it.internalName)?.let { crop ->
                    crop.kind == patch.kind && crop.shipped && crop.level <= player.farmingLvl &&
                        FarmingBags.seedBox.count(player, crop.seed) >= crop.seedsPerPlant
                } == true
            }
            if (seed == null) mes(NOTHING)
            else with(farming) { plant(patch, seed.internalName, fromSeedBox = true) }
            return
        }
        with(farming) {
            when {
                obj == SPADE -> if (!dig(patch)) mes(NOTHING)
                obj == RAKE -> rake(patch)
                obj == PLANT_CURE -> plantCure(patch)
                obj == EMPTY_POT -> fillPot(patch)
                WateringCans.isCan(obj) -> water(patch, obj)
                FarmingCrops.bySeed(obj) != null -> plant(patch, obj)
                else -> mes(NOTHING)
            }
        }
    }

    private companion object {
        const val TIMER_TRANSMIT = "timer.farming_transmit"
        const val TRANSMIT_INTERVAL = 10
        const val EMPTY_POT = "obj.plantpot_empty"
        val HARVEST_OPS = setOf("harvest", "collect")
        val SPIRIT_OPS = setOf("travel", "talk-to", "last-destination")
    }
}

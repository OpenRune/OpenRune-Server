package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingScript
@Inject
constructor(private val xpMods: XpModifiers, private val random: GameRandom) : PluginScript() {
    private val transmitVarbits = HashMap<String, String>()

    override fun ScriptContext.startup() {
        for (patch in FarmingPatches.all) {
            resolveTransmitVarbit(patch)
            onOpLoc1(patch.loc) { interact(patch) }
            onOpLoc2(patch.loc) { interact(patch) }
            onOpLoc3(patch.loc) { clear(patch) }
            onOpLocU(patch.loc) { useItem(patch, it.objType.internalName) }
        }

        onPlayerLogin {
            player.growPatches()
            player.transmitNearest()
            player.softTimer(TIMER_TRANSMIT, TRANSMIT_INTERVAL)
        }

        onPlayerSoftTimer(TIMER_TRANSMIT) {
            player.growPatches()
            player.transmitNearest()
        }
    }

    private fun resolveTransmitVarbit(patch: PatchDef) {
        val type = ServerCacheManager.getObject(patch.loc.asRSCM(RSCMType.LOC)) ?: return
        if (type.multiVarBit <= 0) {
            return
        }
        val name = RSCM.getReverseMapping(RSCMType.VARBIT, type.multiVarBit)
        if (name.isNotBlank()) {
            transmitVarbits[patch.loc] = name
        }
    }

    private fun Player.state(patch: PatchDef): PatchState = PatchState.unpack(vars[patch.varp])

    private fun Player.store(patch: PatchDef, state: PatchState) {
        VarPlayerIntMapSetter.set(this, patch.varp, state.pack())
        val varbit = transmitVarbits[patch.loc] ?: return
        if (FarmingPatches.nearest(coords) === FarmingPatches.areaOf(patch)) {
            VarPlayerIntMapSetter.set(this, varbit, state.transmit(state.crop))
        }
    }

    private fun Player.transmitNearest() {
        val area = FarmingPatches.nearest(coords) ?: return
        for (patch in area.patches) {
            val varbit = transmitVarbits[patch.loc] ?: continue
            val state = state(patch)
            VarPlayerIntMapSetter.set(this, varbit, state.transmit(state.crop))
        }
    }

    private fun Player.growPatches() {
        val now = (System.currentTimeMillis() / MILLIS_PER_MINUTE).toInt()
        val last = vars[VARP_CLOCK]
        VarPlayerIntMapSetter.set(this, VARP_CLOCK, now)
        val elapsed = now - last
        if (last == 0 || elapsed <= 0) {
            return
        }
        for (patch in FarmingPatches.all) {
            val current = state(patch)
            val next = current.advance(current.crop, elapsed) { chance -> random.randomDouble() < chance }
            if (next != current) {
                VarPlayerIntMapSetter.set(this, patch.varp, next.pack())
            }
        }
    }

    private suspend fun ProtectedAccess.interact(patch: PatchDef) {
        player.growPatches()
        val state = player.state(patch)
        val crop = state.crop

        when {
            !state.cleared -> rake(patch, state)
            crop == null -> mes("The patch is empty. Plant a seed in it to grow a crop.")
            state.health == Health.DEAD -> mes("The crop has died. Dig it up with a spade.")
            state.health == Health.DISEASED -> cure(patch, state, crop)
            !state.grown(crop) -> mes("The ${crop.name} is still growing.")
            else -> harvest(patch, state, crop)
        }
    }

    private suspend fun ProtectedAccess.rake(patch: PatchDef, state: PatchState) {
        if (!inv.contains(RAKE)) {
            mes("You need a rake to clear this patch.")
            return
        }

        var current = state
        while (!current.cleared) {
            anim(ANIM_RAKE)
            delay(RAKE_TICKS)
            if (invAdd(inv, WEEDS, 1).failure) {
                mes("You don't have enough inventory space to hold any more weeds.")
                break
            }
            current = current.copy(weeds = current.weeds + 1)
            player.store(patch, current)
            statAdvance(STAT_FARMING, RAKE_XP * xpMods.get(player, STAT_FARMING))
        }
        resetAnim()
    }

    private suspend fun ProtectedAccess.cure(patch: PatchDef, state: PatchState, crop: Crop) {
        if (!inv.contains(PLANT_CURE)) {
            mes("You need plant cure to treat this ${crop.name}.")
            return
        }
        anim(ANIM_CURE)
        delay(2)
        if (invDel(inv, PLANT_CURE, 1).failure) {
            resetAnim()
            return
        }
        player.store(patch, state.copy(health = Health.HEALTHY))
        resetAnim()
        mes("The plant cure completely cures the disease.")
    }

    private suspend fun ProtectedAccess.harvest(patch: PatchDef, state: PatchState, crop: Crop) {
        if (!inv.contains(SPADE) && crop.kind == PatchKind.ALLOTMENT) {
            mes("You need a spade to harvest this patch.")
            return
        }

        var current = state
        while (current.produce > 0) {
            if (inv.freeSpace() < 1) {
                mes("You don't have enough inventory space to hold any more produce.")
                break
            }
            anim(ANIM_HARVEST)
            delay(HARVEST_TICKS)
            if (invAdd(inv, crop.produce, 1).failure) {
                break
            }
            statAdvance(STAT_FARMING, crop.harvestXp * xpMods.get(player, STAT_FARMING))
            current = current.copy(produce = current.produce - 1)
            player.store(patch, current)
        }

        resetAnim()
        if (current.produce <= 0) {
            player.store(patch, PatchState())
            spam("The patch is now empty and full of weeds again.")
        }
    }

    private suspend fun ProtectedAccess.clear(patch: PatchDef) {
        val state = player.state(patch)
        if (state.crop == null) {
            interact(patch)
            return
        }
        if (!inv.contains(SPADE)) {
            mes("You need a spade to clear this patch.")
            return
        }
        anim(ANIM_HARVEST)
        delay(2)
        player.store(patch, PatchState())
        resetAnim()
        mes("You clear the patch.")
    }

    private suspend fun ProtectedAccess.useItem(patch: PatchDef, obj: String) {
        player.growPatches()
        val state = player.state(patch)
        when {
            obj == PLANT_CURE -> {
                val crop = state.crop
                if (crop != null && state.health == Health.DISEASED) {
                    cure(patch, state, crop)
                } else {
                    mes("There is nothing on this patch that needs curing.")
                }
            }
            obj in WATERING_CANS -> water(patch, state, obj)
            Compost.entries.any { it.obj == obj } -> applyCompost(patch, state, obj)
            FarmingCrops.bySeed(obj) != null -> plant(patch, state, FarmingCrops.bySeed(obj)!!)
            else -> mes("Nothing interesting happens.")
        }
    }

    private suspend fun ProtectedAccess.plant(patch: PatchDef, state: PatchState, crop: Crop) {
        if (crop.kind != patch.kind) {
            mes("You can't plant ${crop.name} seeds in this patch.")
            return
        }
        if (!state.cleared) {
            mes("The patch needs to be raked before you can plant anything in it.")
            return
        }
        if (state.crop != null) {
            mes("There is already something growing in this patch.")
            return
        }
        if (player.farmingLvl < crop.level) {
            mes("You need a Farming level of ${crop.level} to plant ${crop.name}.")
            return
        }
        if (!inv.contains(DIBBER)) {
            mes("You need a seed dibber to plant seeds.")
            return
        }
        if (inv.count(crop.seed) < crop.seedsPerPlant) {
            mes("You need ${crop.seedsPerPlant} ${crop.name} seeds to plant this patch.")
            return
        }

        anim(ANIM_PLANT)
        delay(PLANT_TICKS)
        if (invDel(inv, crop.seed, crop.seedsPerPlant).failure) {
            resetAnim()
            return
        }

        val planted =
            state.copy(
                cropIndex = FarmingCrops.index(crop),
                stage = 0,
                watered = false,
                health = Health.HEALTHY,
                produce = 0,
                minutes = crop.stageMinutes,
            )
        player.store(patch, planted)
        statAdvance(STAT_FARMING, crop.plantXp * xpMods.get(player, STAT_FARMING))
        resetAnim()
        spam("You plant the ${crop.name} seeds in the patch.")
    }

    private suspend fun ProtectedAccess.water(patch: PatchDef, state: PatchState, can: String) {
        val charges = WATERING_CANS.indexOf(can)
        val crop = state.crop
        when {
            charges <= 0 -> mes("Your watering can is empty.")
            crop == null -> mes("There is nothing to water on this patch.")
            crop.kind == PatchKind.HERB -> mes("Watering herbs does nothing.")
            state.health != Health.HEALTHY -> mes("Water won't cure your crops.")
            state.grown(crop) -> mes("Your crops are already fully grown.")
            state.watered -> mes("This patch has already been watered.")
            else -> {
                anim(ANIM_WATER)
                delay(2)
                invDel(inv, can, 1)
                invAdd(inv, WATERING_CANS[charges - 1], 1)
                player.store(patch, state.copy(watered = true))
                statAdvance(STAT_FARMING, WATER_XP * xpMods.get(player, STAT_FARMING))
                resetAnim()
                spam("You water the ${crop.name}.")
            }
        }
    }

    private suspend fun ProtectedAccess.applyCompost(
        patch: PatchDef,
        state: PatchState,
        obj: String,
    ) {
        val compost = Compost.entries.first { it.obj == obj }
        when {
            !state.cleared -> mes("The patch needs to be raked first.")
            state.crop != null -> mes("You can only treat an empty patch with compost.")
            state.compost != Compost.NONE -> mes("This patch has already been treated.")
            else -> {
                anim(ANIM_COMPOST)
                delay(2)
                if (invDel(inv, obj, 1).failure) {
                    resetAnim()
                    return
                }
                invAdd(inv, "obj.bucket_empty", 1)
                player.store(patch, state.copy(compost = compost))
                statAdvance(STAT_FARMING, COMPOST_XP * xpMods.get(player, STAT_FARMING))
                resetAnim()
                spam("You treat the patch with compost.")
            }
        }
    }

    private companion object {
        const val VARP_CLOCK = "varp.farming_clock"
        const val TIMER_TRANSMIT = "timer.farming_transmit"
        const val TRANSMIT_INTERVAL = 10
        const val MILLIS_PER_MINUTE = 60_000L

        const val RAKE_TICKS = 3
        const val PLANT_TICKS = 3
        const val HARVEST_TICKS = 3

        const val RAKE_XP = 4.0
        const val WATER_XP = 1.0
        const val COMPOST_XP = 18.0
    }
}

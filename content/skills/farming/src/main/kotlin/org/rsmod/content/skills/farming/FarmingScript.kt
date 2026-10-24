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
            onOpLoc2(patch.loc) { inspect(patch) }
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
        if (type.multiVarBit <= 0) return
        val name = RSCM.getReverseMapping(RSCMType.VARBIT, type.multiVarBit)
        if (name.isNotBlank()) transmitVarbits[patch.loc] = name
    }

    private fun Player.state(patch: PatchDef): PatchState = PatchState.unpack(vars[patch.varp])

    private fun Player.store(patch: PatchDef, state: PatchState) {
        VarPlayerIntMapSetter.set(this, patch.varp, state.pack())
        transmitArea(FarmingPatches.areaOf(patch))
    }

    private fun Player.transmitArea(area: FarmingArea) {
        for (patch in area.patches) {
            val varbit = transmitVarbits[patch.loc] ?: continue
            val state = state(patch)
            VarPlayerIntMapSetter.set(this, varbit, state.transmit(state.crop))
        }
    }

    private fun Player.transmitNearest() {
        FarmingPatches.nearest(coords)?.let { transmitArea(it) }
    }

    private fun Player.refresh(patch: PatchDef): PatchState {
        growPatches()
        transmitArea(FarmingPatches.areaOf(patch))
        return state(patch)
    }

    private fun Player.growPatches() {
        val now = (System.currentTimeMillis() / MILLIS_PER_MINUTE).toInt()
        val last = vars[VARP_CLOCK]
        VarPlayerIntMapSetter.set(this, VARP_CLOCK, now)
        val elapsed = now - last
        if (last == 0 || elapsed <= 0) return
        for (area in FarmingPatches.areas) {
            val states = area.patches.map { state(it) }
            val next = area.advance(states, elapsed) { chance -> random.randomDouble() < chance }
            for (i in area.patches.indices) {
                if (next[i] != states[i]) {
                    VarPlayerIntMapSetter.set(this, area.patches[i].varp, next[i].pack())
                }
            }
        }
    }

    private suspend fun ProtectedAccess.inspect(patch: PatchDef) {
        val state = player.refresh(patch)
        val crop = state.crop
        mes(
            when {
                !state.cleared -> "The patch needs weeding."
                crop == null -> "The patch is empty. Plant a seed in it to grow a crop."
                state.health == Health.DEAD -> "The crop has died. Dig it up with a spade."
                state.health == Health.DISEASED -> "The ${crop.name} is diseased. It needs plant cure."
                state.grown(crop) -> "The ${crop.name} is ready to harvest."
                else -> "The ${crop.name} is still growing."
            }
        )
    }

    private suspend fun ProtectedAccess.interact(patch: PatchDef) {
        val state = player.refresh(patch)
        val crop = state.crop
        when {
            !state.cleared -> rake(patch)
            crop == null -> mes("The patch is empty. Plant a seed in it to grow a crop.")
            state.health == Health.DEAD -> clear(patch, state.cropIndex, crop)
            state.health == Health.DISEASED -> cure(patch, state.cropIndex, crop)
            !state.grown(crop) -> mes("The ${crop.name} is still growing.")
            else -> harvest(patch, state.cropIndex, crop)
        }
    }

    private suspend fun ProtectedAccess.rake(patch: PatchDef) {
        if (!inv.contains(RAKE)) {
            mes("You need a rake to clear this patch.")
            return
        }
        while (true) {
            val before = player.refresh(patch)
            if (before.cleared || before.crop != null || !inv.contains(RAKE)) break
            anim(ANIM_RAKE)
            delay(RAKE_TICKS)
            val current = player.refresh(patch)
            if (current.cleared || current.crop != null || !inv.contains(RAKE)) break
            if (invAdd(inv, WEEDS, 1, strict = true).failure) {
                mes("You don't have enough inventory space to hold any more weeds.")
                break
            }
            player.store(patch, current.copy(weeds = current.weeds + 1, minutes = WEED_MINUTES))
            statAdvance(STAT_FARMING, RAKE_XP * xpMods.get(player, STAT_FARMING))
        }
        resetAnim()
    }

    private suspend fun ProtectedAccess.cure(patch: PatchDef, cropIndex: Int, crop: Crop) {
        if (!inv.contains(PLANT_CURE)) {
            mes("You need plant cure to treat this ${crop.name}.")
            return
        }
        anim(ANIM_CURE)
        delay(2)
        val current = player.refresh(patch)
        if (current.cropIndex != cropIndex || current.health != Health.DISEASED ||
            invDel(inv, PLANT_CURE, 1, strict = true).failure
        ) {
            resetAnim()
            return
        }
        player.store(patch, current.copy(health = Health.HEALTHY))
        resetAnim()
        mes("The plant cure completely cures the disease.")
    }

    private suspend fun ProtectedAccess.harvest(patch: PatchDef, cropIndex: Int, crop: Crop) {
        if (!inv.contains(SPADE) && crop.kind == PatchKind.ALLOTMENT) {
            mes("You need a spade to harvest this patch.")
            return
        }
        while (true) {
            val before = player.refresh(patch)
            if (!before.harvestable(cropIndex, crop)) break
            anim(ANIM_HARVEST)
            delay(HARVEST_TICKS)
            val current = player.refresh(patch)
            if (!current.harvestable(cropIndex, crop) ||
                (crop.kind == PatchKind.ALLOTMENT && !inv.contains(SPADE))
            ) break
            val secateurs = inv.contains(MAGIC_SECATEURS) || worn.contains(MAGIC_SECATEURS)
            val amount =
                if (crop.kind == PatchKind.FLOWER) {
                    crop.harvestAmount(player.farmingLvl, secateurs) { random.of(it) }
                } else 1
            if (invAdd(inv, crop.produce, amount, strict = true).failure) {
                mes("You don't have enough inventory space to hold any more produce.")
                break
            }
            val saveLife =
                crop.kind != PatchKind.FLOWER &&
                    random.of(256) < crop.saveThreshold(player.farmingLvl, secateurs)
            val next = current.harvested(saveLife)
            player.store(patch, next)
            val xp = crop.harvestXp + if (next.cropIndex == 0) crop.plantXp else 0.0
            statAdvance(STAT_FARMING, xp * xpMods.get(player, STAT_FARMING))
            if (next.cropIndex == 0) {
                spam("The patch is now empty.")
                break
            }
        }
        resetAnim()
    }

    private fun PatchState.harvestable(cropIndex: Int, crop: Crop): Boolean =
        this.cropIndex == cropIndex && health == Health.HEALTHY && grown(crop) && produce > 0

    private suspend fun ProtectedAccess.clear(patch: PatchDef, cropIndex: Int, crop: Crop) {
        if (!inv.contains(SPADE)) {
            mes("You need a spade to clear this patch.")
            return
        }
        anim(ANIM_HARVEST)
        delay(2)
        val current = player.refresh(patch)
        if (current.cropIndex == cropIndex && current.health == Health.DEAD && inv.contains(SPADE)) {
            player.store(patch, current.empty())
            statAdvance(STAT_FARMING, crop.plantXp * xpMods.get(player, STAT_FARMING))
            mes("You clear the patch.")
        }
        resetAnim()
    }

    internal suspend fun ProtectedAccess.useItem(patch: PatchDef, obj: String) {
        val state = player.refresh(patch)
        val crop = state.crop
        when {
            obj == SPADE && crop != null && state.health == Health.DEAD ->
                clear(patch, state.cropIndex, crop)
            obj == PLANT_CURE -> {
                if (crop != null && state.health == Health.DISEASED) {
                    cure(patch, state.cropIndex, crop)
                } else {
                    mes("There is nothing on this patch that needs curing.")
                }
            }
            obj in WATERING_CANS -> water(patch, state, obj)
            FarmingComposts.byObj(obj) != null -> applyCompost(patch, state, obj)
            FarmingCrops.bySeed(obj) != null -> plant(patch, state, FarmingCrops.bySeed(obj)!!)
            else -> mes("Nothing interesting happens.")
        }
    }

    private suspend fun ProtectedAccess.plant(patch: PatchDef, state: PatchState, crop: Crop) {
        when {
            crop.kind != patch.kind -> mes("You can't plant ${crop.name} seeds in this patch.")
            !state.cleared -> mes("The patch needs to be raked before you can plant anything in it.")
            state.crop != null -> mes("There is already something growing in this patch.")
            player.farmingLvl < crop.level ->
                mes("You need a Farming level of ${crop.level} to plant ${crop.name}.")
            !inv.contains(DIBBER) -> mes("You need a seed dibber to plant seeds.")
            inv.count(crop.seed) < crop.seedsPerPlant ->
                mes("You need ${crop.seedsPerPlant} ${crop.name} seeds to plant this patch.")
            else -> {
                anim(ANIM_PLANT)
                delay(PLANT_TICKS)
                val current = player.refresh(patch)
                if (!current.cleared || current.crop != null || !inv.contains(DIBBER) ||
                    player.farmingLvl < crop.level ||
                    invDel(inv, crop.seed, crop.seedsPerPlant, strict = true).failure
                ) {
                    resetAnim()
                    return
                }
                player.store(
                    patch,
                    current.copy(
                        cropIndex = FarmingCrops.index(crop),
                        stage = 0,
                        watered = false,
                        health = Health.HEALTHY,
                        produce = 0,
                        minutes = crop.stageMinutes,
                    ),
                )
                resetAnim()
                spam("You plant the ${crop.name} seeds in the patch.")
            }
        }
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
                val current = player.refresh(patch)
                if (current.cropIndex != state.cropIndex || current.health != Health.HEALTHY ||
                    current.grown(crop) || current.watered ||
                    invReplace(inv, can, 1, WATERING_CANS[charges - 1]).failure
                ) {
                    resetAnim()
                    return
                }
                player.store(patch, current.copy(watered = true))
                statAdvance(STAT_FARMING, WATER_XP * xpMods.get(player, STAT_FARMING))
                resetAnim()
                spam("You water the ${crop.name}.")
            }
        }
    }

    private fun PatchState.compostable(): Boolean =
        cleared && compost == Compost.NONE && health != Health.DEAD &&
            (crop?.let { !grown(it) } ?: true)

    private suspend fun ProtectedAccess.applyCompost(patch: PatchDef, state: PatchState, obj: String) {
        val compost = FarmingComposts.byObj(obj) ?: return
        when {
            !state.cleared -> mes("The patch needs to be raked first.")
            state.compost != Compost.NONE -> mes("This patch has already been treated.")
            !state.compostable() -> mes("You can't treat this crop with compost now.")
            else -> {
                anim(ANIM_COMPOST)
                delay(2)
                val current = player.refresh(patch)
                if (current.cropIndex != state.cropIndex || !current.compostable() ||
                    invReplace(inv, obj, 1, "obj.bucket_empty").failure
                ) {
                    resetAnim()
                    return
                }
                player.store(patch, current.copy(compost = compost.kind))
                statAdvance(STAT_FARMING, compost.xp * xpMods.get(player, STAT_FARMING))
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
        const val MAGIC_SECATEURS = "obj.fairy_enchanted_secateurs"
        const val WEED_MINUTES = 5
        const val RAKE_TICKS = 3
        const val PLANT_TICKS = 3
        const val HARVEST_TICKS = 3
        const val RAKE_XP = 4.0
        const val WATER_XP = 1.0
    }
}

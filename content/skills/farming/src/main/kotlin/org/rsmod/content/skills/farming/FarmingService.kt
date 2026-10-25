package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.enums.FarmingEnums
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.player.hands
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.generic.locs.water.WateringCans
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid

/**
 * Owns every patch state transition. Non-suspend operations revalidate the stored state, run the
 * caller's `pay` transaction and only then mutate, so a failed payment changes nothing. Events are
 * published once, after the new state is stored and produce/XP awarded.
 */
@Singleton
class FarmingService
@Inject
constructor(
    private val xpMods: XpModifiers,
    private val random: GameRandom,
    private val eventBus: EventBus,
    private val rewards: FarmingRewards,
    private val equipment: FarmingEquipment,
    private val siteAccess: FarmingSiteAccess,
) {
    private class Part(val patch: PatchDef, val varbit: String, val coord: CoordGrid)

    private val parts: List<Part> by lazy {
        FarmingPatches.all.flatMap { patch ->
            patch.locs.indices.mapNotNull { i ->
                transmitVarbit(patch.locs[i])?.let { Part(patch, it, patch.coords[i]) }
            }
        }
    }

    fun refresh(player: Player, millis: Long = System.currentTimeMillis()) {
        grow(player, millis)
        transmitNearby(player)
    }

    fun state(player: Player, patch: PatchDef): PatchState {
        grow(player, System.currentTimeMillis())
        return stored(player, patch)
    }

    fun flowerProtected(player: Player, patch: PatchDef): Boolean {
        val state = state(player, patch)
        val crop = state.crop ?: return false
        if (patch.kind != PatchKind.ALLOTMENT) return false
        return FarmingPatches.bySite(patch.site).any {
            stored(player, it).protectsAllotment(it, patch, crop)
        }
    }

    fun minimumYield(player: Player, patch: PatchDef): Int? {
        val state = state(player, patch)
        val crop = state.crop ?: return 0
        if (state.health == Health.DEAD) return 0
        if (!crop.harvestShipped) return null
        if (state.grown(crop) && (state.phase == Phase.CHECKED || !crop.checksHealth) &&
            (crop.harvest == HarvestModel.LIVES || crop.harvest == HarvestModel.PICK)
        ) return state.produce
        return when (crop.harvest) {
            HarvestModel.LIVES -> crop.harvestLives ?: FarmingComposts.get(state.compost).harvestLives
            HarvestModel.BATCH -> crop.harvestAmount
            HarvestModel.PICK -> crop.maxProduce
            HarvestModel.CHOP, HarvestModel.NONE -> null
        }
    }

    fun cure(
        player: Player,
        patch: PatchDef,
        expected: PatchState? = null,
        allowPruning: Boolean = false,
        pay: () -> Boolean,
    ): CureResult {
        val state = state(player, patch)
        if (expected != null && state != expected) return CureResult.STALE
        val crop = state.crop
        val refusal =
            when {
                crop == null -> CureResult.EMPTY
                state.health == Health.DEAD -> CureResult.DEAD
                state.health != Health.DISEASED -> CureResult.NOT_DISEASED
                crop.kind.prunes && !allowPruning -> CureResult.PRUNE_ONLY
                else -> null
            }
        if (refusal != null) return refusal
        if (!pay()) return CureResult.UNPAID
        store(player, patch, state.copy(health = Health.HEALTHY))
        return CureResult.CURED
    }

    fun treat(
        player: Player,
        patch: PatchDef,
        compost: Compost,
        expected: PatchState? = null,
        pay: () -> Boolean,
    ): TreatResult {
        val state = state(player, patch)
        if (expected != null && state != expected) return TreatResult.STALE
        val crop = state.crop
        val refusal =
            when {
                crop == null && !state.cleared -> TreatResult.WEEDS
                state.health == Health.DEAD -> TreatResult.DEAD
                crop != null && state.grown(crop) -> TreatResult.GROWN
                state.compost != Compost.NONE -> TreatResult.ALREADY
                else -> null
            }
        if (refusal != null) return refusal
        if (!pay()) return TreatResult.UNPAID
        store(player, patch, state.copy(compost = compost))
        return TreatResult.TREATED
    }

    fun resurrect(
        player: Player,
        patch: PatchDef,
        success: () -> Boolean,
        expected: PatchState? = null,
        pay: () -> Boolean,
    ): ResurrectResult {
        val state = state(player, patch)
        if (expected != null && state != expected) return ResurrectResult.STALE
        val crop = state.crop
        val refusal =
            when {
                crop == null || state.health != Health.DEAD -> ResurrectResult.NOT_DEAD
                crop.kind in RESURRECT_EXCLUDED -> ResurrectResult.EXCLUDED
                state.resurrected -> ResurrectResult.ALREADY_RESURRECTED
                else -> null
            }
        if (refusal != null) return refusal
        if (!pay()) return ResurrectResult.UNPAID
        if (!success()) {
            store(player, patch, state.empty())
            return ResurrectResult.CLEARED
        }
        store(player, patch, state.copy(health = Health.HEALTHY, resurrected = true))
        return ResurrectResult.RESURRECTED
    }

    fun protect(
        player: Player,
        patch: PatchDef,
        expected: PatchState? = null,
        pay: () -> Boolean,
    ): ProtectResult {
        if (siteAccess.denial(player, patch) != null) return ProtectResult.UNPAID
        val state = state(player, patch)
        if (expected != null && state != expected) return ProtectResult.STALE
        val crop = state.crop
        val refusal =
            when {
                crop == null -> ProtectResult.EMPTY
                state.health == Health.DEAD -> ProtectResult.DEAD
                state.health == Health.DISEASED -> ProtectResult.DISEASED
                state.grown(crop) -> ProtectResult.GROWN
                state.paid -> ProtectResult.ALREADY
                else -> null
            }
        if (refusal != null) return refusal
        if (!pay()) return ProtectResult.UNPAID
        store(player, patch, state.copy(paid = true))
        return ProtectResult.PROTECTED
    }

    fun removeTree(player: Player, patch: PatchDef, expected: PatchState, pay: () -> Boolean): Boolean {
        val state = state(player, patch)
        val crop = state.crop ?: return false
        if (state != expected || crop.kind !in REMOVABLE_TREES || !state.grown(crop) ||
            state.phase != Phase.CHECKED || state.health != Health.HEALTHY
        ) return false
        if (!pay()) return false
        store(player, patch, state.empty())
        eventBus.publish(FarmingClearEvent(player, patch, crop, false))
        return true
    }

    /** Gardener or encounter clear back to weeded soil; no XP. */
    fun clear(player: Player, patch: PatchDef): Boolean {
        val state = state(player, patch)
        if (state.crop == null && state.cleared && !state.scarecrow) return false
        store(player, patch, state.empty())
        eventBus.publish(FarmingClearEvent(player, patch, state.crop, state.health == Health.DEAD))
        return true
    }

    /** Compare-and-set for owners that drive their own transitions (Hespori, contracts). */
    fun replace(
        player: Player,
        patch: PatchDef,
        expected: PatchState,
        next: PatchState,
        pay: () -> Boolean = { true },
    ): Boolean {
        if (state(player, patch) != expected) return false
        if (!pay()) return false
        store(player, patch, next)
        return true
    }

    suspend fun ProtectedAccess.inspect(patch: PatchDef) {
        val state = state(player, patch)
        val crop = state.crop
        val status =
            when {
                !state.cleared && crop == null && patch.kind != PatchKind.GRAPE_VINE -> "The patch needs weeding."
                crop == null -> "The patch is empty."
                state.health == Health.DEAD -> "The ${crop.name} has died."
                state.health == Health.DISEASED -> "The ${crop.name} is diseased."
                state.grown(crop) -> "The ${crop.name} is fully grown."
                else -> "The patch has ${crop.name} growing in it and is at state ${state.stage + 1}/${crop.stages + 1}."
            }
        mes("This is ${article(patch.kind.label)}. ${soil(state.compost)} $status")
    }

    suspend fun ProtectedAccess.rake(patch: PatchDef) {
        if (!inv.contains(RAKE)) {
            mes("You need a rake to clear this patch.")
            return
        }
        while (true) {
            val before = state(player, patch)
            if (before.cleared || before.crop != null || !inv.contains(RAKE)) break
            anim(ANIM_RAKE)
            delay(RAKE_TICKS)
            val current = state(player, patch)
            if (current.cleared || current.crop != null || !inv.contains(RAKE)) break
            if (invAdd(inv, WEEDS, 1, strict = true).failure) {
                mes("You don't have enough inventory space to hold any more weeds.")
                break
            }
            store(player, patch, current.copy(weeds = current.weeds + 1))
            award(patch, RAKE_XP)
        }
        resetAnim()
    }

    suspend fun ProtectedAccess.plant(patch: PatchDef, seed: String, fromSeedBox: Boolean = false) {
        siteAccess.denial(player, patch)?.let { return mes(it) }
        val crop = FarmingCrops.bySeed(seed)?.takeIf(Crop::shipped) ?: return mes(NOTHING)
        val sapling = seed.startsWith(POTTED_PREFIX)
        val tool = if (sapling) SPADE else DIBBER
        val state = state(player, patch)
        when {
            crop.kind != patch.kind ->
                mes("You can only plant ${crop.name} ${if (sapling) "saplings" else "seeds"} in ${article(patch.kind.label)}.")
            !state.cleared && patch.kind != PatchKind.GRAPE_VINE -> mes("This patch needs weeding first.")
            state.crop != null || state.scarecrow -> mes("There is already something growing in this patch.")
            crop.kind == PatchKind.GRAPE_VINE && !state.grapePrepared ->
                mes("You need to treat the soil before planting grape seeds.")
            player.farmingLvl < crop.level -> mes("You need a Farming level of ${crop.level} to plant ${crop.name}.")
            crop.kind == PatchKind.SPIRIT_TREE && !FarmingSpiritTrees.canPlant(player) ->
                mes("You cannot grow another spirit tree at your current Farming level.")
            !hasPlantingTool(crop, tool) ->
                mes(if (sapling) "You need a spade to do that." else "You need a seed dibber to plant seeds.")
            seedCount(seed, fromSeedBox) < crop.seedsPerPlant ->
                mes("You need ${crop.seedsPerPlant} ${crop.name} seeds to plant this patch.")
            else -> plantNow(patch, crop, seed, tool, sapling, fromSeedBox)
        }
    }

    private suspend fun ProtectedAccess.plantNow(
        patch: PatchDef, crop: Crop, seed: String, tool: String, sapling: Boolean, fromSeedBox: Boolean,
    ) {
        anim(ANIM_PLANT)
        delay(PLANT_TICKS)
        siteAccess.denial(player, patch)?.let {
            resetAnim()
            mes(it)
            return
        }
        val current = state(player, patch)
        if ((!current.cleared && patch.kind != PatchKind.GRAPE_VINE) || current.crop != null || current.scarecrow ||
            !hasPlantingTool(crop, tool) || player.farmingLvl < crop.level ||
            (crop.kind == PatchKind.GRAPE_VINE && !current.grapePrepared) ||
            (crop.kind == PatchKind.SPIRIT_TREE && !FarmingSpiritTrees.canPlant(player)) ||
            seedCount(seed, fromSeedBox) < crop.seedsPerPlant
        ) {
            resetAnim()
            return
        }
        val count = if (sapling) 1 else equipment.seedCost(player, crop)
        val paid =
            when {
                fromSeedBox -> FarmingBags.seedBox.remove(player, seed, count)
                !sapling || equipment.smashesPlantPot(player) -> invDel(inv, seed, count, strict = true).success
                else -> invReplace(inv, seed, 1, PLANT_POT).success
            }
        resetAnim()
        if (!paid) return
        if (sapling) equipment.saplingPlanted(player) else equipment.planted(player, crop, count)
        store(player, patch, PatchState(
            weeds = PatchState.CLEARED, cropIndex = crop.index, compost = current.compost,
            fertilised = current.fertilised, grapePrepared = current.grapePrepared,
        ))
        if (crop.kind == PatchKind.ANIMA) {
            award(patch, crop.plantXp)
        }
        val name = ServerCacheManager.getItem(seed.asRSCM(RSCMType.OBJ))?.name?.lowercase() ?: crop.name
        val seeds = if (crop.seedsPerPlant == 1) article(name) else "${crop.seedsPerPlant} ${name}s"
        spam("You plant $seeds in the ${patch.kind.label}.")
        eventBus.publish(FarmingPlantEvent(player, patch, crop))
    }

    suspend fun ProtectedAccess.water(patch: PatchDef, can: String) {
        val state = state(player, patch)
        val crop = state.crop
        when {
            crop == null -> mes("There is nothing to water on this patch.")
            state.health != Health.HEALTHY -> mes("Water won't cure your crops.")
            state.grown(crop) -> mes("Your crops are already fully grown.")
            state.watered -> mes("This patch has already been watered.")
            crop.art.watered.isEmpty() -> mes("Nothing interesting happens.")
            else -> {
                anim(ANIM_WATER)
                delay(2)
                val current = state(player, patch)
                resetAnim()
                if (current.cropIndex != state.cropIndex || current.health != Health.HEALTHY ||
                    current.grown(crop) || current.watered
                ) return
                if (!WateringCans.consume(player, can)) {
                    mes("Your watering can is empty.")
                    return
                }
                store(player, patch, current.copy(watered = true))
                // shortcut: inherited 1 XP per watering is unsourced, confirm against a recording.
                award(patch, WATER_XP)
                spam("You water the ${crop.name}.")
            }
        }
    }

    suspend fun ProtectedAccess.plantCure(patch: PatchDef) {
        if (!inv.contains(PLANT_CURE)) {
            mes("You need a plant cure to cure the disease on this patch.")
            return
        }
        anim(ANIM_CURE)
        delay(2)
        resetAnim()
        when (cure(player, patch) { invDel(inv, PLANT_CURE, 1, strict = true).success }) {
            CureResult.CURED -> {
                spam("You treat the ${patch.kind.label} with the plant cure.")
                spam("It is restored to health.")
            }
            CureResult.PRUNE_ONLY, CureResult.UNPAID -> Unit
            else -> mes("There is nothing on this patch that needs curing.")
        }
    }

    /** Check-health pays check and planting XP, fills renewable produce and makes trees choppable. */
    fun ProtectedAccess.checkHealth(patch: PatchDef) {
        val state = state(player, patch)
        val crop = state.crop ?: return
        if (!crop.checksHealth || !state.grown(crop) || state.health != Health.HEALTHY || state.phase != Phase.GROWING) {
            return
        }
        val produce = if (crop.harvest == HarvestModel.PICK) crop.maxProduce else state.produce
        val reward = rewards.prepare(player, crop)
        store(player, patch, state.copy(phase = Phase.CHECKED, produce = produce))
        award(patch, crop.checkXp + crop.plantXp)
        rewards.complete(player, reward)
        mes("You examine the ${crop.name} for signs of disease and find that it is in perfect health.")
        eventBus.publish(FarmingCheckHealthEvent(player, patch, crop))
    }

    /** Returns false when this crop's harvest is not shipped, so the caller can fall through. */
    suspend fun ProtectedAccess.harvest(patch: PatchDef): Boolean {
        val start = state(player, patch)
        val crop = start.crop ?: return false
        if (!crop.harvestShipped) return false
        val spade = crop.kind == PatchKind.ALLOTMENT
        if (spade && !inv.contains(SPADE)) {
            mes("You need a spade to harvest this patch.")
            return true
        }
        if (crop.harvest == HarvestModel.LIVES) spam("You begin to harvest the ${patch.kind.label}.")
        while (true) {
            if (!harvestable(state(player, patch), start.cropIndex, crop)) break
            anim(ANIM_HARVEST)
            delay(HARVEST_TICKS)
            val current = state(player, patch)
            if (!harvestable(current, start.cropIndex, crop) || (spade && !inv.contains(SPADE))) break
            if (crop.kind == PatchKind.BELLADONNA &&
                player.hands?.let { FarmingEnums.farming_belladonna_gloves[getInvObj(it)] } != 1
            ) {
                PlayerPoison.incidentalPoisonHit(player, 2)
                mes("You need protective gloves to pick belladonna.")
                break
            }
            val secateurs = equipment.magicSecateurs(player, crop)
            val attas = attasAlive(player)
            val grape = if (crop.kind == PatchKind.GRAPE_VINE) FarmingGrapes.prepare(player, current, secateurs, attas) else null
            val produce = grape?.produce ?: checkNotNull(crop.produce)
            val amount =
                if (crop.harvest == HarvestModel.BATCH) {
                    crop.harvestAmount(player.farmingLvl, secateurs, attas) { random.of(it) }
                } else {
                    1
                }
            val transaction = if (grape == null) FarmingHarvestStorage.prepareHarvest(player, produce, amount)
                else FarmingGrapes.prepareHarvest(player, grape)
            if (!transaction.success) {
                mes("You don't have enough inventory space to hold any more produce.")
                break
            }
            val next = nextAfterHarvest(current, crop, patch, attas, grape)
            val final = next.cropIndex == 0 || next.health == Health.DEAD
            val reward = if (final && !crop.checksHealth) rewards.prepare(player, crop) else FarmingRewardRoll.NONE
            transaction.commitAll()
            store(player, patch, next)
            val plantXp = if (final && !crop.checksHealth && crop.kind != PatchKind.ANIMA) crop.plantXp else 0.0
            award(patch, crop.harvestXp + plantXp)
            rewards.complete(player, reward)
            eventBus.publish(FarmingHarvestEvent(player, patch, crop, produce, amount, final))
            if (final) {
                if (next.cropIndex == 0) mes("The ${patch.kind.label} is now empty.")
                break
            }
        }
        resetAnim()
        return true
    }

    private fun ProtectedAccess.nextAfterHarvest(
        state: PatchState,
        crop: Crop,
        patch: PatchDef,
        attas: Boolean,
        grape: GrapeHarvest?,
    ): PatchState =
        when (crop.harvest) {
            HarvestModel.LIVES -> {
                val itemPercent = equipment.harvestItemPercent(player, crop)
                val bonus = FarmingPatches.yieldBonus(player, patch)
                val threshold = grape?.saveThreshold ?: crop.saveThreshold(player.farmingLvl, itemPercent, bonus, attas)
                val next = state.harvested(random.of(SAVE_ROLL) < threshold)
                if (grape != null && next.cropIndex == 0) state.copy(health = Health.DEAD, produce = 0) else next
            }
            HarvestModel.PICK ->
                if (state.produce > 1 || crop.checksHealth) state.copy(produce = state.produce - 1) else state.empty()
            else -> state.empty()
        }

    private fun attasAlive(player: Player): Boolean =
        FarmingPatches.all.any { patch ->
            patch.kind == PatchKind.ANIMA &&
                state(player, patch).let { it.crop?.seed == ATTAS_SEED && it.health != Health.DEAD }
        }

    private fun harvestable(state: PatchState, cropIndex: Int, crop: Crop): Boolean =
        state.cropIndex == cropIndex &&
            state.health == Health.HEALTHY &&
            state.grown(crop) &&
            state.produce > 0 &&
            (state.phase == Phase.CHECKED || !crop.checksHealth)

    suspend fun ProtectedAccess.dig(patch: PatchDef): Boolean {
        val state = state(player, patch)
        val crop = state.crop ?: return false
        val dead = state.health == Health.DEAD
        val digUp = state.health == Health.HEALTHY && !state.grown(crop) && multiCrop(patch.kind)
        val clearCactus = crop.kind == PatchKind.CACTUS && state.health == Health.HEALTHY &&
            state.grown(crop) && state.phase == Phase.CHECKED
        if (!dead && !digUp && !clearCactus) return false
        if (!inv.contains(SPADE)) {
            mes("You need a spade to do that.")
            return true
        }
        if (digUp && crop.kind == PatchKind.ANIMA) {
            val confirmed = choice2(
                "Yes, dig it up.", true, "No, leave it.", false, "Dig up this living anima plant?",
            )
            ifClose()
            if (!confirmed) return true
        }
        spam("You start digging the farming patch...")
        anim(ANIM_DIG)
        delay(DIG_TICKS)
        resetAnim()
        val current = state(player, patch)
        if (current != state || !inv.contains(SPADE)) return true
        store(player, patch, current.empty())
        if (dead && crop.kind != PatchKind.ANIMA &&
            !(crop.checksHealth && current.phase == Phase.CHECKED)
        ) award(patch, crop.plantXp)
        spam("You have successfully cleared this patch for new crops.")
        eventBus.publish(FarmingClearEvent(player, patch, crop, dead))
        return true
    }

    suspend fun ProtectedAccess.fillPot(patch: PatchDef) {
        val state = state(player, patch)
        if (state.crop != null || state.scarecrow || !state.cleared) return mes("The patch needs to be empty and weeded first.")
        if (!inv.contains(TROWEL)) return mes("You need a gardening trowel to do that.")
        anim(ANIM_COMPOST)
        delay(FILL_TICKS)
        resetAnim()
        val current = state(player, patch)
        if (current.crop != null || current.scarecrow || !current.cleared || !inv.contains(TROWEL)) return
        invReplace(inv, PLANT_POT, 1, FILLED_POT)
    }

    /** Site XP bonuses (Falador medium diary +10%) apply to every Farming action on that patch. */
    private fun ProtectedAccess.award(patch: PatchDef, xp: Double) = awardPatchXp(player, patch, xp)

    fun awardPatchXp(player: Player, patch: PatchDef, xp: Double) {
        val bonus = 1 + FarmingPatches.xpBonusPercent(player, patch) / 100.0
        player.statAdvance(STAT_FARMING, xp * bonus * xpMods.get(player, STAT_FARMING))
    }

    private fun ProtectedAccess.hasPlantingTool(crop: Crop, tool: String): Boolean =
        (tool == DIBBER && !equipment.needsDibber(player, crop)) || inv.contains(tool)

    private fun ProtectedAccess.seedCount(seed: String, fromSeedBox: Boolean): Int =
        if (!fromSeedBox) inv.count(seed)
        else if (FarmingBags.seedBox.held(player) != null) FarmingBags.seedBox.count(player, seed)
        else 0

    private fun multiCrop(kind: PatchKind): Boolean = FarmingCrops.all.count { it.kind == kind } > 1

    internal fun stored(player: Player, patch: PatchDef): PatchState = PatchState.unpack(player.vars[patch.varp])

    internal fun store(player: Player, patch: PatchDef, state: PatchState) {
        val before = stored(player, patch)
        if (before == state) return
        VarPlayerIntMapSetter.set(player, patch.varp, state.pack())
        transmitNearby(player)
        eventBus.publish(FarmingPatchChangeEvent(player, patch, before, state))
    }

    private fun grow(player: Player, millis: Long) {
        val now = Math.floorDiv(millis, FarmingClock.MINUTE_MILLIS)
        val last = player.vars[VARP_CLOCK].toLong()
        if (now <= last) return
        VarPlayerIntMapSetter.set(player, VARP_CLOCK, now.toInt())
        if (last == 0L) return
        val patches = FarmingPatches.all
        val before = Array(patches.size) { stored(player, patches[it]) }
        val after = before.copyOf()
        val diseaseFree = BooleanArray(patches.size) { FarmingPatches.diseaseFree(player, patches[it]) }
        FarmingGrowth(patches, diseaseFree, random, FarmingAutoWeed.active(player))
            .replay(after, last, now, FarmingClock.offset(player))
        for (i in patches.indices) {
            if (after[i] != before[i]) VarPlayerIntMapSetter.set(player, patches[i].varp, after[i].pack())
        }
        if (!before.contentEquals(after)) eventBus.publish(FarmingRefreshEvent(player, before, after))
    }

    /** Patches sharing a transmit varbit are sent farthest first so the nearest one wins. */
    fun transmitNearby(player: Player) {
        val here = player.coords
        parts
            .filter { it.coord.level == here.level && it.coord.chebyshevDistance(here) <= TRANSMIT_RADIUS }
            .sortedByDescending { it.coord.chebyshevDistance(here) }
            .forEach { part ->
                val state = stored(player, part.patch)
                VarPlayerIntMapSetter.set(player, part.varbit, state.transmit(state.crop, part.patch.kind))
            }
        val hespori = FarmingPatches.all.first { it.kind == PatchKind.HESPORI }
        FarmingHesporiSignals.transmit(player, stored(player, hespori))
    }

    private fun transmitVarbit(loc: String): String? {
        val type = ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)) ?: return null
        if (type.multiVarBit <= 0) return null
        return RSCM.getReverseMapping(RSCMType.VARBIT, type.multiVarBit).takeIf { it.isNotBlank() }
    }

    private companion object {
        const val VARP_CLOCK = "varp.farming_clock"
        const val PLANT_POT = "obj.plantpot_empty"
        const val FILLED_POT = "obj.plantpot_compost"
        const val TROWEL = "obj.gardening_trowel"
        const val POTTED_PREFIX = "obj.plantpot_"
        const val ATTAS_SEED = "obj.attas_seed"
        const val ANIM_DIG = "seq.human_dig"
        const val TRANSMIT_RADIUS = 52
        const val RAKE_TICKS = 3
        const val PLANT_TICKS = 3
        const val HARVEST_TICKS = 3
        const val DIG_TICKS = 3
        const val FILL_TICKS = 3
        const val RAKE_XP = 4.0
        const val WATER_XP = 1.0
        const val SAVE_ROLL = 256
        val RESURRECT_EXCLUDED = setOf(PatchKind.ANIMA, PatchKind.HESPORI, PatchKind.GRAPE_VINE)
        val REMOVABLE_TREES = setOf(PatchKind.TREE, PatchKind.FRUIT_TREE, PatchKind.HARDWOOD_TREE, PatchKind.REDWOOD)

        val PatchKind.label: String
            get() =
                when (this) {
                    PatchKind.ALLOTMENT -> "allotment"
                    else -> name.lowercase().replace('_', ' ') + " patch"
                }

        val Compost.label: String
            get() =
                when (this) {
                    Compost.NORMAL -> "compost"
                    Compost.SUPER -> "supercompost"
                    Compost.ULTRA -> "ultracompost"
                    Compost.NONE -> "nothing"
                }

        fun soil(compost: Compost): String =
            if (compost == Compost.NONE) "The soil has not been treated." else "The soil has been treated with ${compost.label}."

        fun article(noun: String): String = if (noun.first() in "aeiou") "an $noun" else "a $noun"
    }
}

package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.generic.locs.water.WateringCans
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.objtx.TransactionResultList

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

    fun cure(
        player: Player,
        patch: PatchDef,
        expected: PatchState? = null,
        pay: () -> Boolean,
    ): CureResult {
        val state = state(player, patch)
        if (expected != null && state != expected) return CureResult.STALE
        val refusal =
            when {
                state.crop == null -> CureResult.EMPTY
                state.health == Health.DEAD -> CureResult.DEAD
                state.health != Health.DISEASED -> CureResult.NOT_DISEASED
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

    suspend fun ProtectedAccess.inspect(patch: PatchDef) {
        val state = state(player, patch)
        val crop = state.crop
        val status =
            when {
                !state.cleared && crop == null -> "The patch needs weeding."
                crop == null -> "The patch is empty."
                state.health == Health.DEAD -> "The ${crop.name} has died."
                state.health == Health.DISEASED -> "The ${crop.name} is diseased."
                state.grown(crop) -> "The ${crop.name} is fully grown."
                else -> "The patch has ${crop.name} growing in it and is at state ${state.stage + 1}/${crop.stages + 1}."
            }
        mes("This is ${article(patch.kind.label)}. ${soil(state.compost)} $status")
    }

    /**
     * One rake stroke per call. The weed lands when [actionDelay] is reached and the next stroke is
     * re-triggered, so walking away or clicking elsewhere cancels the rest.
     */
    fun ProtectedAccess.rake(patch: PatchDef, bound: BoundLocInfo) {
        val active = actionDelay >= mapClock
        val state = state(player, patch)
        if (!inv.contains(RAKE)) {
            if (!active) mes("You need a rake to clear this patch.")
            return resetAnim()
        }
        if (state.cleared || state.crop != null) return resetAnim()
        if (actionDelay != mapClock) {
            if (!active) {
                actionDelay = mapClock + RAKE_TICKS
                anim(ANIM_RAKE)
            }
            return opLoc1(bound)
        }
        if (invAdd(inv, WEEDS, 1, strict = true).failure) {
            mes("You don't have enough inventory space to hold any more weeds.")
            return resetAnim()
        }
        val next = state.copy(weeds = state.weeds + 1)
        store(player, patch, next)
        award(patch, RAKE_XP)
        if (next.cleared) return resetAnim()
        actionDelay = mapClock + RAKE_TICKS
        anim(ANIM_RAKE)
        opLoc1(bound)
    }

    suspend fun ProtectedAccess.plant(patch: PatchDef, seed: String) {
        siteAccess.denial(player, patch)?.let { return mes(it) }
        val crop = FarmingCrops.bySeed(seed)?.takeIf(Crop::shipped) ?: return mes(NOTHING)
        val state = state(player, patch)
        when {
            crop.kind != patch.kind -> mes("You can only plant ${crop.name} seeds in ${article(patch.kind.label)}.")
            !state.cleared -> mes("This patch needs weeding first.")
            state.crop != null -> mes("There is already something growing in this patch.")
            player.farmingLvl < crop.level -> mes("You need a Farming level of ${crop.level} to plant ${crop.name}.")
            !inv.contains(DIBBER) -> mes("You need a seed dibber to plant seeds.")
            inv.count(seed) < crop.seedsPerPlant ->
                mes("You need ${crop.seedsPerPlant} ${crop.name} seeds to plant this patch.")
            else -> plantNow(patch, crop, seed)
        }
    }

    private suspend fun ProtectedAccess.plantNow(patch: PatchDef, crop: Crop, seed: String) {
        anim(ANIM_PLANT)
        soundSynth(SOUND_PLANT)
        delay(PLANT_TICKS)
        siteAccess.denial(player, patch)?.let {
            resetAnim()
            mes(it)
            return
        }
        val current = state(player, patch)
        if (!current.cleared || current.crop != null || !inv.contains(DIBBER) ||
            player.farmingLvl < crop.level || inv.count(seed) < crop.seedsPerPlant
        ) {
            resetAnim()
            return
        }
        if (!invDel(inv, seed, crop.seedsPerPlant, strict = true).success) return
        store(player, patch, PatchState(weeds = PatchState.CLEARED, cropIndex = crop.index, compost = current.compost))
        val name = ServerCacheManager.getItem(seed.asRSCM(RSCMType.OBJ))?.name?.lowercase() ?: crop.name
        val seeds = if (crop.seedsPerPlant == 1) article(name) else "${crop.seedsPerPlant} ${name}s"
        spam("You plant $seeds in the ${patch.kind.label}.")
        eventBus.publish(FarmingPlantEvent(player, patch, crop))
    }

    suspend fun ProtectedAccess.water(patch: PatchDef, can: String, canSlot: Int) {
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
                if (!WateringCans.consume(player, can, canSlot)) {
                    mes("Your watering can is empty.")
                    return
                }
                store(player, patch, current.copy(watered = true))
                // UNVERIFIED: 1 XP per watering (guess)
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
        val before = state(player, patch)
        // Dry run: UNPAID means the patch can be cured; payment waits until the cure lands.
        when (cure(player, patch, before) { false }) {
            CureResult.UNPAID -> Unit
            else -> return mes("There is nothing on this patch that needs curing.")
        }
        spam("You treat the ${patch.kind.label} with the plant cure.")
        anim(ANIM_CURE)
        soundSynth(SOUND_CURE)
        delay(3)
        val paid = cure(player, patch, before) { invReplace(inv, PLANT_CURE, 1, VIAL).success }
        if (paid == CureResult.CURED) spam("It is restored to health.")
    }

    /**
     * One pick per call, re-triggered like woodcutting so walking away or clicking elsewhere
     * cancels the rest. Returns false when this crop's harvest is not shipped, so the caller can
     * fall through.
     */
    fun ProtectedAccess.harvest(patch: PatchDef, bound: BoundLocInfo): Boolean {
        val active = actionDelay >= mapClock
        val current = state(player, patch)
        val crop = current.crop ?: return active
        if (!crop.harvestShipped) return false
        val spade = crop.kind == PatchKind.ALLOTMENT
        val recorded = spade || crop.kind == PatchKind.HERB
        fun stop(): Boolean {
            if (!recorded) resetAnim()
            return true
        }
        if (spade && !inv.contains(SPADE)) {
            if (!active) mes("You need a spade to harvest this patch.")
            return stop()
        }
        if (!harvestable(current, crop)) return stop()
        if (actionDelay != mapClock) {
            if (!active) {
                if (crop.harvest == HarvestModel.LIVES) spam("You begin to harvest the ${patch.kind.label}.")
                actionDelay = mapClock + if (recorded) 1 else HARVEST_TICKS
                pickAnim(spade)
            }
            opLoc1(bound)
            return true
        }
        val produce = checkNotNull(crop.produce)
        val amount =
            if (crop.harvest == HarvestModel.BATCH) crop.harvestAmount(player.farmingLvl) { random.of(it) } else 1
        val transaction = prepareHarvest(player, produce, amount)
        if (!transaction.success) {
            mes("You don't have enough inventory space to hold any more produce.")
            return stop()
        }
        val next = nextAfterHarvest(current, crop, patch)
        val final = next.cropIndex == 0
        transaction.commitAll()
        store(player, patch, next)
        if (crop.kind == PatchKind.HERB) soundSynth(SOUND_PICK)
        award(patch, crop.harvestXp + if (final) crop.plantXp else 0.0)
        eventBus.publish(FarmingHarvestEvent(player, patch, crop, produce, amount, final))
        if (final) {
            mes("The ${patch.kind.label} is now empty.")
            return stop()
        }
        if (recorded && !prepareHarvest(player, produce, 1).success) {
            spam("You have run out of inventory space.")
            return stop()
        }
        actionDelay = mapClock + HARVEST_TICKS
        pickAnim(spade)
        opLoc1(bound)
        return true
    }

    private fun ProtectedAccess.pickAnim(spade: Boolean) {
        anim(if (spade) ANIM_DIG else ANIM_HARVEST)
        if (spade) soundSynth(SOUND_DIG)
    }

    private fun prepareHarvest(player: Player, produce: String, amount: Int): TransactionResultList<InvObj> =
        player.invTransaction(player.inv, autoCommit = false) {
            add(select(player.inv), produce.asRSCM(RSCMType.OBJ), amount)
        }

    private fun ProtectedAccess.nextAfterHarvest(state: PatchState, crop: Crop, patch: PatchDef): PatchState =
        when (crop.harvest) {
            HarvestModel.LIVES -> {
                val threshold = crop.saveThreshold(player.farmingLvl, FarmingPatches.yieldBonus(player, patch))
                state.harvested(random.of(SAVE_ROLL) < threshold)
            }
            HarvestModel.BATCH -> state.empty()
        }

    private fun harvestable(state: PatchState, crop: Crop): Boolean =
        state.health == Health.HEALTHY &&
            state.grown(crop) &&
            state.produce > 0

    suspend fun ProtectedAccess.dig(patch: PatchDef): Boolean {
        val state = state(player, patch)
        val crop = state.crop ?: return false
        val dead = state.health == Health.DEAD
        val digUp = state.health == Health.HEALTHY && !state.grown(crop) && multiCrop(patch.kind)
        if (!dead && !digUp) return false
        if (!inv.contains(SPADE)) {
            mes("You need a spade to do that.")
            return true
        }
        spam("You start digging the farming patch...")
        // UNVERIFIED: live often digs more 4-tick cycles before clearing, more at low Farming; odds unknown (recording tallies)
        anim(ANIM_DIG)
        soundSynth(SOUND_DIG)
        delay(4)
        anim(ANIM_DIG)
        soundSynth(SOUND_DIG)
        delay(2)
        val current = state(player, patch)
        if (current != state || !inv.contains(SPADE)) return true
        store(player, patch, current.empty())
        if (dead) award(patch, crop.plantXp)
        spam("You have successfully cleared this patch for new crops.")
        eventBus.publish(FarmingClearEvent(player, patch, crop, dead))
        return true
    }

    /** Site XP bonuses (Falador medium diary +10%) apply to every Farming action on that patch. */
    private fun ProtectedAccess.award(patch: PatchDef, xp: Double) = awardPatchXp(player, patch, xp)

    fun awardPatchXp(player: Player, patch: PatchDef, xp: Double) {
        val bonus = 1 + FarmingPatches.xpBonusPercent(player, patch) / 100.0
        player.statAdvance(STAT_FARMING, xp * bonus * xpMods.get(player, STAT_FARMING))
    }

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
        FarmingGrowth(patches, diseaseFree, random).replay(after, last, now, FarmingClock.offset(player))
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
                VarPlayerIntMapSetter.set(player, part.varbit, state.transmit(state.crop))
            }
    }

    private fun transmitVarbit(loc: String): String? {
        val type = ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)) ?: return null
        if (type.multiVarBit <= 0) return null
        return RSCM.getReverseMapping(RSCMType.VARBIT, type.multiVarBit).takeIf { it.isNotBlank() }
    }

    private companion object {
        const val VARP_CLOCK = "varp.farming_clock"
        const val VIAL = "obj.vial_empty"
        const val ANIM_DIG = "seq.human_dig"
        const val TRANSMIT_RADIUS = 52

        // UNVERIFIED: live raking cycles every 4 ticks and yields a weed on roughly a third of cycles; this is a guaranteed weed every 3
        const val RAKE_TICKS = 3
        const val PLANT_TICKS = 3
        const val HARVEST_TICKS = 3
        const val RAKE_XP = 4.0
        const val WATER_XP = 1.0
        const val SAVE_ROLL = 256

        fun soil(compost: Compost): String =
            if (compost == Compost.NONE) "The soil has not been treated." else "The soil has been treated with ${compost.label}."

        fun article(noun: String): String = if (noun.first() in "aeiou") "an $noun" else "a $noun"
    }
}

package org.rsmod.content.skills.farming

import org.rsmod.api.random.GameRandom

/**
 * Replays the global farming clock over every patch. A crop with cycle `C` advances at each minute
 * `m` where `(m + offset) % C == 0`; weeds regrow on the 5-minute tick. Disease replaces the
 * stage advance; diseased crops use their sourced `deathMinutes` boundary.
 */
internal class FarmingGrowth(
    private val patches: List<PatchDef>,
    private val diseaseFree: BooleanArray,
    private val random: GameRandom,
    private val autoWeed: Boolean = false,
) {
    fun replay(states: Array<PatchState>, fromMinute: Long, toMinute: Long, offset: Int) {
        var minute = fromMinute + 1
        minute += Math.floorMod(-(minute + offset), WEED_MINUTES.toLong())
        val protected = BooleanArray(states.size)
        while (minute <= toMinute && states.indices.any {
            active(states[it], !autoWeed && patches[it].kind != PatchKind.GRAPE_VINE)
        }) {
            for (i in states.indices) protected[i] = flowerProtected(states, i)
            tick(states, minute + offset, protected)
            minute += WEED_MINUTES
        }
    }

    private fun tick(states: Array<PatchState>, time: Long, protected: BooleanArray) {
        val iasor = patches.indices.any { states[it].isLiving(IASOR_SEED) }
        for (i in states.indices) {
            states[i] = advance(states[i], time, diseaseFree[i] || protected[i], iasor, patches[i].kind != PatchKind.GRAPE_VINE)
        }
    }

    private fun advance(state: PatchState, time: Long, immune: Boolean, iasor: Boolean, weedsGrow: Boolean): PatchState {
        val crop = state.crop ?: return if (!autoWeed && weedsGrow && state.weeds > 0) state.copy(weeds = state.weeds - 1) else state
        if (state.health == Health.DEAD || state.phase == Phase.STUMP) return state
        val cycle = crop.cycleMinutes.toLong()
        if (state.health == Health.DISEASED) {
            val death = crop.deathMinutes ?: return state
            return if (time % death == 0L) state.copy(health = Health.DEAD, watered = false) else state
        }
        if (state.grown(crop)) return grown(state, crop, time)
        val stageMinutes = if (state.stage == 0 && crop.firstStageMinutes > 0) crop.firstStageMinutes.toLong() else cycle
        if (time % stageMinutes != 0L) return state
        if (!immune && diseases(state, crop, iasor)) {
            return state.copy(health = Health.DISEASED, watered = false)
        }
        val next = state.copy(stage = state.stage + 1, watered = false)
        if (!next.grown(crop)) return next
        // Native anima art counts seedling as stage 1: index 7 withers, index 8 dies.
        return if (crop.kind == PatchKind.ANIMA) next.copy(health = Health.DEAD) else next.matured(crop)
    }

    private fun grown(state: PatchState, crop: Crop, time: Long): PatchState =
        if (regrows(state, crop) && time % crop.regrowMinutes == 0L) state.copy(produce = state.produce + 1) else state

    /** No crop rolls disease in its first stage; gardener protection blocks every roll. */
    private fun diseases(state: PatchState, crop: Crop, iasor: Boolean): Boolean {
        if (state.stage < 1 || state.paid) return false
        if (crop.diseaseLegacy) {
            // shortcut: preserve the inherited watered-stage protection and approximate rate;
            // replace together once the actual per-crop watering reduction is sourced.
            if (state.watered) return false
            return random.randomDouble() < FarmingComposts.get(state.compost).diseaseChance / crop.stages
        }
        val max = crop.diseaseMax ?: return false
        val scaled = max / FarmingComposts.get(state.compost).diseaseDivisor
        return random.of(128) <= if (iasor) scaled / 5 else scaled
    }

    /** Allotments are protected by a grown, healthy matching flower (or white lily) on the same site. */
    private fun flowerProtected(states: Array<PatchState>, index: Int): Boolean {
        val patch = patches[index]
        if (patch.kind != PatchKind.ALLOTMENT) return false
        val crop = states[index].crop ?: return false
        return patches.indices.any { i ->
            states[i].protectsAllotment(patches[i], patch, crop)
        }
    }

    companion object {
        const val WEED_MINUTES: Int = 5
        private const val IASOR_SEED = "obj.iasor_seed"

        fun active(state: PatchState, weedsGrow: Boolean = true): Boolean {
            val crop = state.crop ?: return weedsGrow && state.weeds > 0
            return when {
                state.health == Health.DEAD || state.phase == Phase.STUMP -> false
                state.health == Health.DISEASED || !state.grown(crop) -> true
                else -> regrows(state, crop)
            }
        }

        private fun regrows(state: PatchState, crop: Crop): Boolean =
            crop.harvest == HarvestModel.PICK &&
                crop.regrowMinutes > 0 &&
                state.produce < crop.maxProduce &&
                (state.phase == Phase.CHECKED || !crop.checksHealth)

        private fun PatchState.isLiving(seed: String): Boolean =
            crop?.seed == seed && health != Health.DEAD
    }
}

internal fun PatchState.protectsAllotment(flowerPatch: PatchDef, allotment: PatchDef, crop: Crop): Boolean {
    if (flowerPatch.kind != PatchKind.FLOWER || flowerPatch.site != allotment.site) return false
    if (scarecrow) return crop.seed == "obj.sweetcorn_seed"
    val flower = this.crop ?: return false
    return health == Health.HEALTHY && grown(flower) &&
        (flower.seed == "obj.white_lily_seed" || flower.index == crop.protectionIndex)
}

/** Starting produce once the final stage is reached. */
internal fun PatchState.matured(crop: Crop): PatchState =
    when (crop.harvest) {
        HarvestModel.LIVES -> copy(produce = crop.harvestLives ?: FarmingComposts.get(compost).harvestLives)
        HarvestModel.BATCH -> copy(produce = 1)
        HarvestModel.PICK -> if (crop.checksHealth) this else copy(produce = crop.maxProduce)
        HarvestModel.CHOP, HarvestModel.NONE -> this
    }

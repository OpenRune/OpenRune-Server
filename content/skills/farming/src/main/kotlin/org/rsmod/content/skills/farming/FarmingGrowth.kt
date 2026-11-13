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
) {
    fun replay(states: Array<PatchState>, fromMinute: Long, toMinute: Long, offset: Int) {
        var minute = fromMinute + 1
        minute += Math.floorMod(-(minute + offset), WEED_MINUTES.toLong())
        val protected = BooleanArray(states.size)
        while (minute <= toMinute && states.any { active(it) }) {
            for (i in states.indices) protected[i] = flowerProtected(states, i)
            tick(states, minute + offset, protected)
            minute += WEED_MINUTES
        }
    }

    private fun tick(states: Array<PatchState>, time: Long, protected: BooleanArray) {
        for (i in states.indices) {
            states[i] = advance(states[i], time, diseaseFree[i] || protected[i])
        }
    }

    private fun advance(state: PatchState, time: Long, immune: Boolean): PatchState {
        val crop = state.crop ?: return if (state.weeds > 0) state.copy(weeds = state.weeds - 1) else state
        if (state.health == Health.DEAD) return state
        if (state.health == Health.DISEASED) {
            val death = crop.deathMinutes ?: return state
            return if (time % death == 0L) state.copy(health = Health.DEAD, watered = false) else state
        }
        if (state.grown(crop)) return state
        if (time % crop.cycleMinutes != 0L) return state
        if (!immune && diseases(state, crop)) {
            return state.copy(health = Health.DISEASED, watered = false)
        }
        val next = state.copy(stage = state.stage + 1, watered = false)
        return if (next.grown(crop)) next.matured(crop) else next
    }

    /** No crop rolls disease in its first stage. */
    private fun diseases(state: PatchState, crop: Crop): Boolean {
        if (state.stage < 1) return false
        if (crop.diseaseLegacy) {
            // UNVERIFIED: watered stages are fully protected; real per-crop reduction unknown (guess)
            if (state.watered) return false
            return random.randomDouble() < FarmingComposts.get(state.compost).diseaseChance / crop.stages
        }
        val max = crop.diseaseMax ?: return false
        val scaled = max / FarmingComposts.get(state.compost).diseaseDivisor
        return random.of(128) <= scaled
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

        fun active(state: PatchState): Boolean {
            val crop = state.crop ?: return state.weeds > 0
            return state.health == Health.DISEASED || (state.health != Health.DEAD && !state.grown(crop))
        }
    }
}

internal fun PatchState.protectsAllotment(flowerPatch: PatchDef, allotment: PatchDef, crop: Crop): Boolean {
    if (flowerPatch.kind != PatchKind.FLOWER || flowerPatch.site != allotment.site) return false
    val flower = this.crop ?: return false
    return health == Health.HEALTHY && grown(flower) &&
        (flower.seed == "obj.white_lily_seed" || flower.index == crop.protectionIndex)
}

/** Starting produce once the final stage is reached. */
internal fun PatchState.matured(crop: Crop): PatchState =
    when (crop.harvest) {
        HarvestModel.LIVES -> copy(produce = FarmingComposts.get(compost).harvestLives)
        HarvestModel.BATCH -> copy(produce = 1)
    }

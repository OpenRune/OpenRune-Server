package org.rsmod.content.skills.farming

enum class Health {
    HEALTHY,
    DISEASED,
    DEAD,
}

/**
 * One patch, packed into a single varp so it persists with the rest of the character.
 *
 * [weeds] counts rake strokes: 0 is fully overgrown and [CLEARED] is ready to plant. [minutes] is
 * how much real time is left before the next growth stage, which is what lets a patch keep growing
 * while its owner is logged out - the elapsed time is handed to [advance] on the next login.
 * [produce] stores remaining harvest lives, or one pending flower batch.
 */
data class PatchState(
    val weeds: Int = 0,
    val cropIndex: Int = 0,
    val stage: Int = 0,
    val watered: Boolean = false,
    val health: Health = Health.HEALTHY,
    val compost: Compost = Compost.NONE,
    val produce: Int = 0,
    val minutes: Int = 0,
) {
    val cleared: Boolean
        get() = weeds >= CLEARED

    fun grown(crop: Crop): Boolean = stage >= crop.stages

    fun pack(): Int =
        (weeds and 0x3 shl 28) or
            (minutes.coerceIn(0, 255) shl 20) or
            (produce.coerceIn(0, 15) shl 16) or
            (compost.ordinal shl 14) or
            (health.ordinal shl 12) or
            (if (watered) 1 shl 11 else 0) or
            (stage.coerceIn(0, 15) shl 7) or
            cropIndex.coerceIn(0, 127)

    /**
     * Runs [elapsed] minutes of growth. A patch that is diseased when its next stage comes due dies
     * instead of advancing, which is the window the player has to cure it.
     */
    fun advance(
        crop: Crop?,
        elapsed: Int,
        compost: CompostDef,
        `protected`: Boolean = false,
        diseaseRoll: (Double) -> Boolean,
    ): PatchState {
        if (elapsed <= 0) return this
        if (crop == null) {
            var state = this
            var left = elapsed
            while (state.weeds > 0) {
                val remaining = state.minutes.takeIf { it > 0 } ?: WEED_MINUTES
                if (left < remaining) return state.copy(minutes = remaining - left)
                left -= remaining
                state = state.copy(weeds = state.weeds - 1, minutes = 0)
                if (state.weeds > 0) state = state.copy(minutes = WEED_MINUTES)
                if (left == 0) return state
            }
            return state
        }
        if (health == Health.DEAD || grown(crop)) return this

        var state = this
        var left = elapsed
        while (left > 0 && !state.grown(crop)) {
            if (state.minutes > left) {
                return state.copy(minutes = state.minutes - left)
            }
            left -= state.minutes

            if (state.health == Health.DISEASED) {
                return state.copy(health = Health.DEAD, minutes = 0)
            }

            val diseased =
                !`protected` &&
                    !state.watered &&
                    state.stage + 1 < crop.stages &&
                    // shortcut: inherited stage/watering model; replace when exact cycle rates are known.
                    diseaseRoll(compost.diseaseChance / crop.stages)

            state =
                state.copy(
                    stage = state.stage + 1,
                    watered = false,
                    minutes = crop.stageMinutes,
                    health = if (diseased) Health.DISEASED else Health.HEALTHY,
                )
        }

        if (state.grown(crop)) {
            state =
                state.copy(
                    minutes = 0,
                    produce = if (crop.kind == PatchKind.FLOWER) 1 else compost.harvestLives,
                )
        }
        return state
    }

    /** The value the client reads to pick this patch's appearance out of its transform table. */
    fun transmit(crop: Crop?): Int {
        if (crop == null) return weeds
        if (crop.transmit.isNotEmpty()) {
            val bank =
                when {
                    health == Health.DEAD -> 3
                    health == Health.DISEASED -> 2
                    watered -> 1
                    else -> 0
                }
            return crop.transmit[bank * (crop.stages + 1) + stage.coerceIn(0, crop.stages)]
        }
        return when (crop.kind) {
            PatchKind.HERB ->
                when (health) {
                    Health.DEAD -> crop.deadBase + (stage - 1).coerceIn(0, 2)
                    Health.DISEASED -> crop.diseasedBase + (stage - 1).coerceIn(0, 2)
                    Health.HEALTHY -> crop.growBase + stage
                }
            else ->
                when {
                    health == Health.DEAD -> crop.deadBase + stage - 1
                    health == Health.DISEASED -> crop.diseasedBase + stage - 1
                    watered -> crop.wateredBase + stage
                    else -> crop.growBase + stage
                }
        }
    }

    companion object {
        const val CLEARED: Int = 3
        private const val WEED_MINUTES = 5

        fun unpack(value: Int): PatchState =
            PatchState(
                weeds = (value ushr 28) and 0x3,
                minutes = (value ushr 20) and 0xFF,
                produce = (value ushr 16) and 0xF,
                compost = Compost.entries[(value ushr 14) and 0x3],
                health = Health.entries[((value ushr 12) and 0x3).coerceAtMost(Health.entries.lastIndex)],
                watered = ((value ushr 11) and 0x1) == 1,
                stage = (value ushr 7) and 0xF,
                cropIndex = value and 0x7F,
            )
    }
}

fun Crop.saveThreshold(farmingLevel: Int, magicSecateurs: Boolean): Int {
    val level = farmingLevel.coerceIn(1, 99)
    val low = if (magicSecateurs) saveLow * 110 / 100 else saveLow
    val high = if (magicSecateurs) saveHigh * 110 / 100 else saveHigh
    return 1 + (low * (99 - level) + high * (level - 1) + 49) / 98
}

fun Crop.harvestAmount(farmingLevel: Int, magicSecateurs: Boolean, roll: (Int) -> Int): Int {
    if (harvestLevelDivisor == 0) return harvestAmount
    val level = if (magicSecateurs) farmingLevel * 110 / 100 else farmingLevel
    return harvestAmount + roll(level.coerceAtLeast(1)) / harvestLevelDivisor
}

fun PatchState.harvested(saveLife: Boolean): PatchState =
    when {
        saveLife -> this
        produce > 1 -> copy(produce = produce - 1)
        else -> empty()
    }

fun PatchState.empty(): PatchState = PatchState(weeds = PatchState.CLEARED, minutes = 5)

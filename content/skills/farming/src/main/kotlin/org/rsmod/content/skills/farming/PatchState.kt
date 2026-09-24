package org.rsmod.content.skills.farming

import kotlin.math.min

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
    fun advance(crop: Crop?, elapsed: Int, diseaseRoll: (Double) -> Boolean): PatchState {
        if (crop == null || health == Health.DEAD || grown(crop)) {
            return this
        }

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
                !state.watered &&
                    state.stage + 1 < crop.stages &&
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
            state = state.copy(minutes = 0, produce = harvestsFor(crop))
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
                    Health.DEAD -> HERB_DEAD_BASE + min(stage - 1, 2).coerceAtLeast(0)
                    Health.DISEASED -> crop.diseasedBase + (stage - 1).coerceIn(0, 2)
                    Health.HEALTHY -> crop.growBase + stage
                }
            else -> {
                val base = crop.growBase + stage
                when {
                    health == Health.DEAD -> base + DEAD_OFFSET
                    health == Health.DISEASED -> base + DISEASED_OFFSET
                    watered -> base + WATERED_OFFSET
                    else -> base
                }
            }
        }
    }

    private fun harvestsFor(crop: Crop): Int =
        when (crop.kind) {
            PatchKind.FLOWER -> 1
            else -> BASE_HARVESTS + compost.ordinal
        }

    companion object {
        const val CLEARED: Int = 3
        const val BASE_HARVESTS: Int = 3

        private const val WATERED_OFFSET = 64
        private const val DISEASED_OFFSET = 128
        private const val DEAD_OFFSET = 192
        private const val HERB_DEAD_BASE = 170

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

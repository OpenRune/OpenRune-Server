package org.rsmod.content.skills.farming

enum class Health {
    HEALTHY,
    DISEASED,
    DEAD,
}

/** What a grown crop is doing: still unchecked or harvesting, checked for health, or a stump. */
enum class Phase {
    GROWING,
    CHECKED,
    STUMP,
}

/**
 * One patch, packed into a single varp so it persists with the rest of the character.
 *
 * [weeds] counts rake strokes: 0 is fully overgrown and [CLEARED] is ready to plant. Growth is
 * driven by the global farming clock, so the state carries no timer. [produce] stores remaining
 * harvest lives or one pending batch.
 *
 * Packed layout (bit 31 marks it): crop 0-6, stage 7-10, watered 11, health 12-13, compost 14-15,
 * produce 16-19, phase 20-21, weeds 28-29. Bits 22-27 are reserved for later features and are
 * never reused for anything else. Legacy bits 20-27 held a countdown and are dropped without the
 * version bit.
 */
data class PatchState(
    val weeds: Int = 0,
    val cropIndex: Int = 0,
    val stage: Int = 0,
    val watered: Boolean = false,
    val health: Health = Health.HEALTHY,
    val compost: Compost = Compost.NONE,
    val produce: Int = 0,
    val phase: Phase = Phase.GROWING,
) {
    val cleared: Boolean
        get() = weeds >= CLEARED

    fun grown(crop: Crop): Boolean = stage >= crop.stages

    fun pack(): Int =
        VERSION or
            (weeds and 0x3 shl 28) or
            (phase.ordinal shl 20) or
            (produce.coerceIn(0, 15) shl 16) or
            (compost.ordinal shl 14) or
            (health.ordinal shl 12) or
            (if (watered) 1 shl 11 else 0) or
            (stage.coerceIn(0, 15) shl 7) or
            cropIndex.coerceIn(0, 127)

    /** The value the client reads to pick this patch's appearance out of its transform table. */
    fun transmit(crop: Crop?): Int {
        if (crop == null) return weeds
        val art = crop.art
        val stage = stage.coerceIn(0, crop.stages)
        return when {
            health == Health.DEAD && art.dead.isNotEmpty() -> art.dead[stage]
            health == Health.DISEASED && art.diseased.isNotEmpty() -> art.diseased[stage]
            watered && art.watered.isNotEmpty() -> art.watered[stage]
            else -> art.healthy[stage]
        }
    }

    companion object {
        const val CLEARED: Int = 3
        private const val VERSION = 1 shl 31

        fun unpack(value: Int): PatchState {
            val current = value and VERSION != 0
            return PatchState(
                weeds = (value ushr 28) and 0x3,
                phase = if (current) Phase.entries.getOrElse((value ushr 20) and 0x3) { Phase.GROWING } else Phase.GROWING,
                produce = (value ushr 16) and 0xF,
                compost = Compost.entries[(value ushr 14) and 0x3],
                health = Health.entries[((value ushr 12) and 0x3).coerceAtMost(Health.entries.lastIndex)],
                watered = ((value ushr 11) and 0x1) == 1,
                stage = (value ushr 7) and 0xF,
                cropIndex = value and 0x7F,
            )
        }
    }
}

fun PatchState.empty(): PatchState = PatchState(weeds = PatchState.CLEARED)

/**
 * Chance to save a harvest life out of 256 (Farming: `1 + floor(low(99-L)/98 + high(L-1)/98 +
 * 0.5)`). Diary [bonus] adds flat to each endpoint.
 */
fun Crop.saveThreshold(farmingLevel: Int, bonus: Int = 0): Int {
    val level = farmingLevel.coerceIn(1, 99)
    val low = checkNotNull(saveLow) + bonus
    val high = checkNotNull(saveHigh) + bonus
    return 1 + Math.floorDiv(low * (99 - level) + high * (level - 1) + 49, 98)
}

/** Batch size for [HarvestModel.BATCH] crops: limpwurt rolls `[0, L-1]` over [harvestLevelDivisor]. */
fun Crop.harvestAmount(farmingLevel: Int, roll: (Int) -> Int): Int {
    if (harvestLevelDivisor == 0) return harvestAmount
    return harvestAmount + roll(farmingLevel.coerceAtLeast(1)) / harvestLevelDivisor
}

fun PatchState.harvested(saveLife: Boolean): PatchState =
    when {
        saveLife -> this
        produce > 1 -> copy(produce = produce - 1)
        else -> empty()
    }

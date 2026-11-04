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
 * harvest lives, renewable produce or one pending batch. [paid] is gardener protection.
 *
 * Packed layout (bit 31 marks it): crop 0-6, stage 7-10, watered 11, health 12-13, compost 14-15,
 * produce 16-19, phase 20-21, paid 22, resurrected 23, scarecrow 24, fertilised 25, grapePrepared
 * 26, weeds 28-29. Legacy bits 20-27 held a countdown and are dropped without the version bit.
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
    val paid: Boolean = false,
    val resurrected: Boolean = false,
    val scarecrow: Boolean = false,
    val fertilised: Boolean = false,
    val grapePrepared: Boolean = false,
) {
    val cleared: Boolean
        get() = weeds >= CLEARED

    fun grown(crop: Crop): Boolean = stage >= crop.stages

    fun pack(): Int =
        VERSION or
            (weeds and 0x3 shl 28) or
            (if (grapePrepared) 1 shl 26 else 0) or
            (if (fertilised) 1 shl 25 else 0) or
            (if (scarecrow) 1 shl 24 else 0) or
            (if (resurrected) 1 shl 23 else 0) or
            (if (paid) 1 shl 22 else 0) or
            (phase.ordinal shl 20) or
            (produce.coerceIn(0, 15) shl 16) or
            (compost.ordinal shl 14) or
            (health.ordinal shl 12) or
            (if (watered) 1 shl 11 else 0) or
            (stage.coerceIn(0, 15) shl 7) or
            cropIndex.coerceIn(0, 127)

    /** The value the client reads to pick this patch's appearance out of its transform table. */
    fun transmit(crop: Crop?, kind: PatchKind): Int {
        if (scarecrow) return FarmingScarecrow.transmit(weeds)
        if (kind == PatchKind.GRAPE_VINE && crop == null) return if (grapePrepared) 1 else 0
        if (crop == null) return weeds
        val art = crop.art
        val stage = stage.coerceIn(0, crop.stages)
        return when {
            health == Health.DEAD && art.dead.isNotEmpty() -> art.dead[stage]
            health == Health.DISEASED && art.diseased.isNotEmpty() -> art.diseased[stage]
            phase == Phase.STUMP && art.stump != null -> art.stump
            art.produce.isNotEmpty() && showsProduce(crop) -> art.produce[produce.coerceIn(0, crop.maxProduce)]
            watered && art.watered.isNotEmpty() -> art.watered[stage]
            else -> art.healthy[stage]
        }
    }

    private fun showsProduce(crop: Crop): Boolean =
        grown(crop) && (phase == Phase.CHECKED || !crop.checksHealth)

    companion object {
        const val CLEARED: Int = 3
        private const val VERSION = 1 shl 31

        fun unpack(value: Int): PatchState {
            val current = value and VERSION != 0
            return PatchState(
                weeds = (value ushr 28) and 0x3,
                resurrected = current && (value ushr 23) and 0x1 == 1,
                scarecrow = current && (value ushr 24) and 0x1 == 1,
                fertilised = current && (value ushr 25) and 0x1 == 1,
                grapePrepared = current && (value ushr 26) and 0x1 == 1,
                paid = current && (value ushr 22) and 0x1 == 1,
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
 * 0.5)`). Item boosts (magic secateurs 10%, Farming cape 5%) add up and floor each endpoint
 * once; diary [bonus] then adds flat (Mod Lenny, 5 Sep 2023); a living Attas plant then floors
 * each endpoint times 1.05 (published wiki model, Farming/HFC/FYC).
 */
fun Crop.saveThreshold(farmingLevel: Int, itemPercent: Int = 0, bonus: Int = 0, attas: Boolean = false): Int {
    val level = farmingLevel.coerceIn(1, 99)
    fun boost(cts: Int): Int {
        val diary = cts * (100 + itemPercent) / 100 + bonus
        return if (attas) Math.floorDiv(diary * ATTAS_PERCENT, 100) else diary
    }
    val low = boost(checkNotNull(saveLow))
    val high = boost(checkNotNull(saveHigh))
    return 1 + Math.floorDiv(low * (99 - level) + high * (level - 1) + 49, 98)
}

/**
 * Batch size for [HarvestModel.BATCH] crops: limpwurt rolls `[0, L-1]`, belladonna `[0, L]`, where
 * L is the level floored after adding magic secateurs 10% and Attas 5% (FYC model).
 */
fun Crop.harvestAmount(farmingLevel: Int, magicSecateurs: Boolean, attas: Boolean = false, roll: (Int) -> Int): Int {
    if (harvestLevelDivisor == 0) return harvestAmount
    val percent = 100 + (if (magicSecateurs && this.magicSecateurs) 10 else 0) + (if (attas) 5 else 0)
    val boosted = farmingLevel * percent / 100
    val bound = if (kind == PatchKind.BELLADONNA) boosted + 1 else boosted
    return harvestAmount + roll(bound.coerceAtLeast(1)) / harvestLevelDivisor
}

private const val ATTAS_PERCENT = 105

fun PatchState.harvested(saveLife: Boolean): PatchState =
    when {
        saveLife -> this
        produce > 1 -> copy(produce = produce - 1)
        else -> empty()
    }

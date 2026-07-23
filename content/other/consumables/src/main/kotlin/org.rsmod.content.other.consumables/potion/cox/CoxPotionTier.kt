package org.rsmod.content.other.consumables.potion.cox

import org.rsmod.api.table.PotionEffectRow

internal enum class CoxPotionTier(
    val clientTier: Int,
    val combatConstant: Int,
    val combatPercent: Int,
    val revitalisationConstant: Int,
    val revitalisationPercent: Int,
    val xericsAidHealConstant: Int,
    val xericsAidHealPercent: Int,
    val xericsAidDefenceConstant: Int,
    val xericsAidDefencePercent: Int,
    val xericsAidDrainConstant: Int,
    val xericsAidDrainPercent: Int,
    val prayerEnhancePercent: Int,
    val prayerEnhanceConstant: Int,
) {
    Weak(
        clientTier = 0,
        combatConstant = 4,
        combatPercent = 10,
        revitalisationConstant = 5,
        revitalisationPercent = 20,
        xericsAidHealConstant = 2,
        xericsAidHealPercent = 10,
        xericsAidDefenceConstant = 2,
        xericsAidDefencePercent = 15,
        xericsAidDrainConstant = 2,
        xericsAidDrainPercent = 8,
        prayerEnhancePercent = 30,
        prayerEnhanceConstant = 13,
    ),

    Normal(
        clientTier = 1,
        combatConstant = 5,
        combatPercent = 13,
        revitalisationConstant = 8,
        revitalisationPercent = 25,
        xericsAidHealConstant = 3,
        xericsAidHealPercent = 12,
        xericsAidDefenceConstant = 3,
        xericsAidDefencePercent = 18,
        xericsAidDrainConstant = 2,
        xericsAidDrainPercent = 9,
        prayerEnhancePercent = 40,
        prayerEnhanceConstant = 22,
    ),

    Strong(
        clientTier = 2,
        combatConstant = 6,
        combatPercent = 16,
        revitalisationConstant = 11,
        revitalisationPercent = 30,
        xericsAidHealConstant = 5,
        xericsAidHealPercent = 15,
        xericsAidDefenceConstant = 5,
        xericsAidDefencePercent = 20,
        xericsAidDrainConstant = 4,
        xericsAidDrainPercent = 10,
        prayerEnhancePercent = 50,
        prayerEnhanceConstant = 31,
    ),
    ;

    fun prayerEnhanceActivations(
        basePrayer: Int,
    ): Int {
        return basePrayer *
            prayerEnhancePercent /
            100 +
            prayerEnhanceConstant
    }

    companion object {
        fun from(effect: PotionEffectRow): CoxPotionTier =
            when (effect.variant) {
                VARIANT_WEAK -> Weak
                VARIANT_NORMAL -> Normal
                VARIANT_STRONG -> Strong

                else ->
                    error(
                        "Unsupported Chambers of Xeric potion " +
                            "variant '${effect.variant}' for " +
                            "'${effect.key}'.",
                    )
            }

        private const val VARIANT_WEAK: String =
            "weak"

        private const val VARIANT_NORMAL: String =
            "normal"

        private const val VARIANT_STRONG: String =
            "strong"
    }
}

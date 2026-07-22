package org.rsmod.content.other.consumables.potion.cox

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.table.PotionEffectRow
import org.rsmod.content.other.consumables.potion.PotionStatBoost
import org.rsmod.content.other.consumables.potion.applyStatBoost
import org.rsmod.content.other.consumables.potion.drainCurrentStats
import org.rsmod.content.other.consumables.potion.restoreIfDrained
import org.rsmod.game.entity.Player

@Singleton
class CoxPotionEffect
@Inject
constructor(
    private val prayerEnhance: CoxPrayerEnhanceEffect,
    private val overload: CoxOverloadEffect,
) {
    fun handles(
        handler: String,
    ): Boolean =
        handler in HANDLERS

    fun canApply(
        access: ProtectedAccess,
        effect: PotionEffectRow,
    ): Boolean =
        when (effect.handler) {
            HANDLER_OVERLOAD ->
                overload.canApply(access)

            else -> true
        }

    fun apply(
        access: ProtectedAccess,
        effect: PotionEffectRow,
    ) {
        val tier =
            CoxPotionTier.from(effect)

        with(access) {
            when (effect.handler) {
                HANDLER_XERICS_AID ->
                    applyXericsAid(tier)

                HANDLER_REVITALISATION ->
                    applyRevitalisation(tier)

                HANDLER_PRAYER_ENHANCE ->
                    prayerEnhance.apply(
                        access = this,
                        tier = tier,
                    )

                HANDLER_OVERLOAD ->
                    overload.apply(
                        access = this,
                        tier = tier,
                    )

                HANDLER_ELDER_POTION ->
                    applyCombatBoost(
                        stats = ELDER_POTION_STATS,
                        tier = tier,
                    )

                HANDLER_KODAI_POTION ->
                    applyCombatBoost(
                        stats = KODAI_POTION_STATS,
                        tier = tier,
                    )

                HANDLER_TWISTED_POTION ->
                    applyCombatBoost(
                        stats = TWISTED_POTION_STATS,
                        tier = tier,
                    )

                else ->
                    error(
                        "Unsupported Chambers of Xeric potion handler: " +
                            "'${effect.handler}'.",
                    )
            }
        }
    }

    fun clearSessionEffects(
        player: Player,
    ) {
        prayerEnhance.clear(player)
        overload.clear(player)
    }

    private fun ProtectedAccess.applyXericsAid(
        tier: CoxPotionTier,
    ) {
        statBoost(
            stat = HITPOINTS,
            constant = tier.xericsAidHealConstant,
            percent = tier.xericsAidHealPercent,
        )

        statBoost(
            stat = DEFENCE,
            constant = tier.xericsAidDefenceConstant,
            percent = tier.xericsAidDefencePercent,
        )

        drainCurrentStats(
            stats = XERICS_AID_DRAIN_STATS,
            constant = tier.xericsAidDrainConstant,
            percent = tier.xericsAidDrainPercent,
        )
    }

    private fun ProtectedAccess.applyRevitalisation(
        tier: CoxPotionTier,
    ) {
        ServerCacheManager
            .getStats()
            .values
            .forEach { stat ->
                val internalName =
                    stat.internalName

                if (internalName == HITPOINTS) {
                    return@forEach
                }

                restoreIfDrained(
                    stat = internalName,
                    constant =
                        tier.revitalisationConstant,
                    percent =
                        tier.revitalisationPercent,
                )
            }
    }

    private fun ProtectedAccess.applyCombatBoost(
        stats: Iterable<String>,
        tier: CoxPotionTier,
    ) {
        applyStatBoost(
            stats = stats,
            boost =
                PotionStatBoost(
                    constant = tier.combatConstant,
                    percent = tier.combatPercent,
                ),
        )
    }

    companion object {
        private const val HITPOINTS: String =
            "stat.hitpoints"

        private const val ATTACK: String =
            "stat.attack"

        private const val STRENGTH: String =
            "stat.strength"

        private const val DEFENCE: String =
            "stat.defence"

        private const val RANGED: String =
            "stat.ranged"

        private const val MAGIC: String =
            "stat.magic"

        private const val HANDLER_XERICS_AID: String =
            "cox_xerics_aid"

        private const val HANDLER_REVITALISATION: String =
            "cox_revitalisation"

        private const val HANDLER_PRAYER_ENHANCE: String =
            "cox_prayer_enhance"

        private const val HANDLER_OVERLOAD: String =
            "cox_overload"

        private const val HANDLER_ELDER_POTION: String =
            "cox_elder_potion"

        private const val HANDLER_KODAI_POTION: String =
            "cox_kodai_potion"

        private const val HANDLER_TWISTED_POTION: String =
            "cox_twisted_potion"

        private val HANDLERS: Set<String> =
            setOf(
                HANDLER_XERICS_AID,
                HANDLER_REVITALISATION,
                HANDLER_PRAYER_ENHANCE,
                HANDLER_OVERLOAD,
                HANDLER_ELDER_POTION,
                HANDLER_KODAI_POTION,
                HANDLER_TWISTED_POTION,
            )

        private val ELDER_POTION_STATS: List<String> =
            listOf(
                ATTACK,
                STRENGTH,
                DEFENCE,
            )

        private val KODAI_POTION_STATS: List<String> =
            listOf(
                MAGIC,
                DEFENCE,
            )

        private val TWISTED_POTION_STATS: List<String> =
            listOf(
                RANGED,
                DEFENCE,
            )

        private val XERICS_AID_DRAIN_STATS: List<String> =
            listOf(
                ATTACK,
                STRENGTH,
                RANGED,
                MAGIC,
            )
    }
}

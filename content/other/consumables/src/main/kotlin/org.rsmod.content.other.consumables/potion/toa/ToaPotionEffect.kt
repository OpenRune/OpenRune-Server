package org.rsmod.content.other.consumables.potion.toa

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.table.PotionEffectRow
import org.rsmod.content.other.consumables.potion.drainCurrentStats
import org.rsmod.content.other.consumables.potion.restoreIfDrained
import org.rsmod.game.entity.Player

@Singleton
class ToaPotionEffect
@Inject
constructor(
    private val smellingSalts: ToaSmellingSaltsEffect,
    private val liquidAdrenaline: ToaLiquidAdrenalineEffect,
    private val overTimeEffects: ToaOverTimeEffect,
) {
    fun handles(handler: String): Boolean =
        handler in HANDLERS

    fun apply(
        access: ProtectedAccess,
        effect: PotionEffectRow,
    ) {
        with(access) {
            when (effect.handler) {
                HANDLER_NECTAR ->
                    applyNectar()

                HANDLER_AMBROSIA ->
                    applyAmbrosia()

                HANDLER_TEARS_OF_ELIDINIS ->
                    applyTearsOfElidinis()

                HANDLER_LIQUID_ADRENALINE ->
                    liquidAdrenaline.apply(this)

                HANDLER_SMELLING_SALTS ->
                    smellingSalts.apply(this)

                HANDLER_SILK_DRESSING ->
                    overTimeEffects.applySilkDressing(this)

                HANDLER_BLESSED_CRYSTAL_SCARAB ->
                    overTimeEffects.applyBlessedCrystalScarab(this)

                else ->
                    error(
                        "Unsupported Tombs of Amascut potion handler: " +
                            "'${effect.handler}'.",
                    )
            }
        }
    }

    /**
     * Clears every active Tombs-specific timed effect.
     *
     * The future raid session should call this when the player dies,
     * leaves, or finishes the raid.
     */
    fun clearSessionEffects(
        player: Player,
    ) {
        smellingSalts.clear(player)
        liquidAdrenaline.clear(player)
        overTimeEffects.clear(player)
    }

    private fun ProtectedAccess.applyNectar() {
        statBoost(
            stat = HITPOINTS,
            constant = NECTAR_HEAL_CONSTANT,
            percent = NECTAR_HEAL_PERCENT,
        )

        drainCurrentStats(
            stats = NECTAR_DRAIN_STATS,
            constant = NECTAR_DRAIN_CONSTANT,
            percent = NECTAR_DRAIN_PERCENT,
        )
    }

    private fun ProtectedAccess.applyAmbrosia() {
        restoreToBoostedTarget(
            stat = HITPOINTS,
            percent = AMBROSIA_HITPOINTS_PERCENT,
            constant = AMBROSIA_HITPOINTS_CONSTANT,
        )

        restoreToBoostedTarget(
            stat = PRAYER,
            percent = AMBROSIA_PRAYER_PERCENT,
            constant = AMBROSIA_PRAYER_CONSTANT,
        )

        restoreFullRunEnergy()
    }

    private fun ProtectedAccess.restoreToBoostedTarget(
        stat: String,
        percent: Int,
        constant: Int,
    ) {
        val base =
            player.statBase(stat)

        val target =
            base +
                base * percent / 100 +
                constant

        val current =
            player.stat(stat)

        if (current >= target) {
            return
        }

        statAdd(
            stat = stat,
            constant = target - current,
            percent = 0,
        )
    }

    private fun ProtectedAccess.restoreFullRunEnergy() {
        if (player.runEnergy >= constants.run_max_energy) {
            return
        }

        player.runEnergy =
            constants.run_max_energy

        UpdateRun.energy(
            player,
            player.runEnergy,
        )
    }

    private fun ProtectedAccess.applyTearsOfElidinis() {
        TEARS_RESTORE_STATS.forEach { stat ->
            restoreIfDrained(
                stat = stat,
                constant = TEARS_STAT_CONSTANT,
                percent = TEARS_STAT_PERCENT,
            )
        }

        restoreIfDrained(
            stat = PRAYER,
            constant = TEARS_PRAYER_CONSTANT,
            percent = TEARS_PRAYER_PERCENT,
        )
    }

    companion object {
        private const val NECTAR_HEAL_CONSTANT: Int = 3
        private const val NECTAR_HEAL_PERCENT: Int = 15
        private const val NECTAR_DRAIN_PERCENT: Int = 20
        private const val NECTAR_DRAIN_CONSTANT: Int = 5

        private const val AMBROSIA_HITPOINTS_PERCENT: Int = 25
        private const val AMBROSIA_HITPOINTS_CONSTANT: Int = 2
        private const val AMBROSIA_PRAYER_PERCENT: Int = 20
        private const val AMBROSIA_PRAYER_CONSTANT: Int = 5

        private const val TEARS_STAT_CONSTANT: Int = 3
        private const val TEARS_STAT_PERCENT: Int = 25
        private const val TEARS_PRAYER_CONSTANT: Int = 10
        private const val TEARS_PRAYER_PERCENT: Int = 25

        private const val HITPOINTS: String =
            "stat.hitpoints"

        private const val PRAYER: String =
            "stat.prayer"

        private const val HANDLER_NECTAR: String =
            "toa_nectar"

        private const val HANDLER_AMBROSIA: String =
            "toa_ambrosia"

        private const val HANDLER_TEARS_OF_ELIDINIS: String =
            "toa_tears_of_elidinis"

        private const val HANDLER_LIQUID_ADRENALINE: String =
            "toa_liquid_adrenaline"

        private const val HANDLER_SMELLING_SALTS: String =
            "toa_smelling_salts"

        private const val HANDLER_SILK_DRESSING: String =
            "toa_silk_dressing"

        private const val HANDLER_BLESSED_CRYSTAL_SCARAB: String =
            "toa_blessed_crystal_scarab"

        private val HANDLERS: Set<String> =
            setOf(
                HANDLER_NECTAR,
                HANDLER_AMBROSIA,
                HANDLER_TEARS_OF_ELIDINIS,
                HANDLER_LIQUID_ADRENALINE,
                HANDLER_SMELLING_SALTS,
                HANDLER_SILK_DRESSING,
                HANDLER_BLESSED_CRYSTAL_SCARAB,
            )

        private val NECTAR_DRAIN_STATS: List<String> =
            listOf(
                "stat.attack",
                "stat.strength",
                "stat.defence",
                "stat.ranged",
                "stat.magic",
            )

        private val TEARS_RESTORE_STATS: List<String> =
            listOf(
                "stat.attack",
                "stat.strength",
                "stat.defence",
                "stat.ranged",
                "stat.magic",
            )
    }
}

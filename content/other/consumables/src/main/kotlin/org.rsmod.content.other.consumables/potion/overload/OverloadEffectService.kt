package org.rsmod.content.other.consumables.potion.overload

import jakarta.inject.Singleton
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.other.consumables.potion.PotionStatBoost
import org.rsmod.content.other.consumables.potion.applyStatBoost
import org.rsmod.content.other.consumables.potion.durationUnits
import org.rsmod.content.other.consumables.potion.nextTimedEffectDelay
import org.rsmod.content.other.consumables.potion.playPotionVisual
import org.rsmod.content.other.consumables.potion.restartTimer
import org.rsmod.content.other.consumables.potion.restoreHitpointsIfDrained
import org.rsmod.content.other.consumables.potion.restoreStatsToBase
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

@Singleton
class OverloadEffectService {
    internal fun canApply(
        access: ProtectedAccess,
        type: OverloadType,
    ): Boolean {
        val definition =
            OverloadRegistry[type]

        return with(access) {
            if (player.hitpoints <= definition.totalDamage) {
                mes(
                    "You need more than " +
                        "${definition.totalDamage} Hitpoints " +
                        "to drink this potion.",
                )
                false
            } else {
                true
            }
        }
    }

    internal fun apply(
        access: ProtectedAccess,
        type: OverloadType,
        boost: PotionStatBoost? = null,
        clientTier: Int = 0,
    ) {
        val definition =
            OverloadRegistry[type]

        val resolvedBoost =
            boost
                ?: requireNotNull(
                    definition.defaultBoost,
                ) {
                    "No boost configured for overload type '$type'."
                }

        with(access) {
            applyStatBoost(
                stats = definition.boostedStats,
                boost = resolvedBoost,
            )

            player.attr[definition.stateKey] =
                OverloadState(
                    expiresAt =
                        mapClock + definition.duration,
                    boost = resolvedBoost,
                    damageHitsRemaining =
                        definition.damageHits,
                    warned = false,
                )

            startDisplay(
                player = player,
                definition = definition,
                clientTier = clientTier,
            )

            player.restartTimer(
                timerKey = definition.damageTimer,
                delay = definition.damageInterval,
            )

            player.restartTimer(
                timerKey = definition.effectTimer,
                delay = definition.refreshInterval,
            )
        }
    }

    internal fun processDamage(
        access: ProtectedAccess,
        type: OverloadType,
    ) {
        val definition =
            OverloadRegistry[type]

        with(access) {
            val state =
                player.attr[definition.stateKey]
                    ?: return

            if (state.damageHitsRemaining <= 0) {
                return
            }

            val remainingHits =
                state.damageHitsRemaining - 1

            player.attr[definition.stateKey] =
                state.copy(
                    damageHitsRemaining =
                        remainingHits,
                )

            playPotionVisual(
                animation = definition.animation,
                spotAnimation =
                    definition.spotAnimation,
            )

            takeInstantHit(
                type = HitType.Typeless,
                damage = definition.damagePerHit,
            )

            if (remainingHits > 0) {
                player.timer(
                    definition.damageTimer,
                    definition.damageInterval,
                )
            }
        }
    }

    internal fun process(
        access: ProtectedAccess,
        type: OverloadType,
    ) {
        val definition =
            OverloadRegistry[type]

        with(access) {
            var state =
                player.attr[definition.stateKey]
                    ?: return

            val remaining =
                state.expiresAt - mapClock

            if (remaining <= 0) {
                clear(
                    player = player,
                    type = type,
                )

                restoreHitpointsIfDrained(
                    definition.totalDamage,
                )

                mes(definition.expiryMessage)
                return
            }

            if (
                !state.warned &&
                remaining <= definition.warningLead
            ) {
                mes(definition.warningMessage)

                state =
                    state.copy(
                        warned = true,
                    )

                player.attr[definition.stateKey] =
                    state
            }

            applyStatBoost(
                stats = definition.boostedStats,
                boost = state.boost,
            )

            VarPlayerIntMapSetter.set(
                player,
                definition.timerVarbit,
                durationUnits(
                    duration = remaining,
                    interval =
                        definition.refreshInterval,
                ),
            )

            player.timer(
                definition.effectTimer,
                nextTimedEffectDelay(
                    remaining = remaining,
                    refreshInterval =
                        definition.refreshInterval,
                    warningLead =
                        definition.warningLead,
                    warned = state.warned,
                ),
            )
        }
    }

    internal fun clear(
        player: Player,
        type: OverloadType,
    ) {
        val definition =
            OverloadRegistry[type]

        val active =
            player.attr[definition.stateKey] != null

        player.attr.remove(
            definition.stateKey,
        )

        player.clearTimer(
            definition.effectTimer,
        )

        player.clearTimer(
            definition.damageTimer,
        )

        VarPlayerIntMapSetter.set(
            player,
            definition.timerVarbit,
            0,
        )

        definition.tierVarbit?.let { varbit ->
            VarPlayerIntMapSetter.set(
                player,
                varbit,
                0,
            )
        }

        if (active) {
            player.restoreStatsToBase(
                definition.boostedStats,
            )
        }
    }

    private fun startDisplay(
        player: Player,
        definition: OverloadDefinition,
        clientTier: Int,
    ) {
        VarPlayerIntMapSetter.set(
            player,
            definition.timerVarbit,
            0,
        )

        definition.tierVarbit?.let { varbit ->
            VarPlayerIntMapSetter.set(
                player,
                varbit,
                clientTier,
            )
        }

        VarPlayerIntMapSetter.set(
            player,
            definition.timerVarbit,
            durationUnits(
                duration = definition.duration,
                interval =
                    definition.refreshInterval,
            ),
        )

        definition.startClientScript?.let { script ->
            player.runClientScript(script)
        }
    }
}

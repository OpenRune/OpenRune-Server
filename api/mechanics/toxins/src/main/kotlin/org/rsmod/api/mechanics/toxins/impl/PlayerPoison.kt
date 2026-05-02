package org.rsmod.api.mechanics.toxins.impl

import org.rsmod.api.config.refs.hitmark_groups
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varps
import org.rsmod.api.mechanics.toxins.Toxin
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.getInvObj

public object PlayerPoison {

    public const val TICK_INTERVAL: Int = 30

    public fun isPoisoned(player: Player): Boolean = player.vars[varps.poison_severity] > 0

    public fun damageForSeverity(severity: Int): Int = (severity + 4) / 5

    public fun severityForInitialDamage(initialDamage: Int): Int =
        if (initialDamage <= 0) 0 else 5 * initialDamage - 4

    public fun tryPoison(player: Player, source: Npc): Boolean =
        tryPoison(player, initialDamage = 0, source)

    public fun tryPoison(player: Player, initialDamage: Int = 0, source: Npc): Boolean {
        val fromParam = source.visType.paramOrNull(params.npc_poison_severity) ?: 0
        return applyPoison(player, initialDamage, fromParam)
    }

    public fun tryPoison(player: Player, initialDamage: Int = 0, severity: Int = 0): Boolean =
        applyPoison(player, initialDamage, paramSeverity = severity)

    /**
     * A single poison-type hitsplat using [initialDamage] as literal damage. Does not apply ongoing
     * poison (no severity varp, no timer, no game message). For scenery, traps, nettles, etc.
     */
    public fun incidentalPoisonHit(player: Player, initialDamage: Int): Boolean {
        if (initialDamage <= 0) return false
        queuePoisonHit(player, initialDamage)
        return true
    }

    public fun hasWornPoisonEnvenomImmunity(player: Player): Boolean {
        for (obj in player.worn) {
            if (obj == null) continue
            val type = getInvObj(obj)
            if ((type.paramOrNull(params.worn_poison_envenom_immunity) ?: 0) > 0) {
                return true
            }
        }
        return false
    }

    private fun applyPoison(player: Player, initialDamage: Int, paramSeverity: Int): Boolean {
        if (PlayerVenom.isEnvenomed(player)) {
            return false
        }
        if (hasWornPoisonEnvenomImmunity(player)) {
            return false
        }
        val storedSeverity = when {
            paramSeverity > 0 -> paramSeverity
            initialDamage > 0 -> severityForInitialDamage(initialDamage)
            else -> 0
        }
        if (storedSeverity <= 0) return false

        val incomingDamage = damageForSeverity(storedSeverity)
        val current = player.vars[varps.poison_severity]
        if (current > 0) {
            val currentDamage = damageForSeverity(current)
            if (incomingDamage < currentDamage) return false
            if (incomingDamage == currentDamage && storedSeverity <= current) return false
        }

        VarPlayerIntMapSetter.set(player, varps.poison_severity, storedSeverity)
        val firstHitDamage = if (initialDamage > 0) initialDamage else damageForSeverity(storedSeverity)
        queuePoisonHit(player, firstHitDamage)

        val severity = storedSeverity - 1
        if (severity <= 0) {
            clear(player)
        } else {
            VarPlayerIntMapSetter.set(player, varps.poison_severity, severity)
            player.timer(timers.player_poison, TICK_INTERVAL)
        }
        player.mes("You have been poisoned!", ChatType.Spam)
        Toxin.syncStatusOrbs(player)
        return true
    }

    private fun queuePoisonHit(player: Player, damage: Int) {
        player.queueHit(
            delay = 1,
            type = HitType.Typeless,
            damage = damage,
            hitmark = hitmark_groups.poison_damage,
            modifier = NoopPlayerHitModifier,
        )
    }

    public fun clear(player: Player) {
        VarPlayerIntMapSetter.set(player, varps.poison_severity, 0)
        player.clearTimer(timers.player_poison)
        Toxin.syncStatusOrbs(player)
    }

    public fun onPoisonTimerTick(player: Player) {
        var severity = player.vars[varps.poison_severity]
        if (severity <= 0) {
            clear(player)
            return
        }

        queuePoisonHit(player, damageForSeverity(severity))

        severity--
        if (severity <= 0) {
            clear(player)
        } else {
            VarPlayerIntMapSetter.set(player, varps.poison_severity, severity)
        }
    }
}

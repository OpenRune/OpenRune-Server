package org.rsmod.api.combat.commons

import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statSub
import org.rsmod.game.entity.Player

public object CombatEffects {

    public fun poison(target: Player, damage: Int) {
        PlayerPoison.tryPoison(target, initialDamage = damage)
    }

    private const val FREEZE_IMMUNITY_TICKS = 5

    public fun freeze(target: Player, ticks: Int) {
        if (target.isFrozen) return
        if (target.freezeImmune) return
        target.frozen = true
        target.routeDestination.clear()
        target.timer("timer.combat_freeze", ticks)
        target.mes("You have been frozen!", ChatType.Spam)
    }

    public fun unfreeze(target: Player) {
        target.frozen = false
        target.freezeImmune = true
        target.clearTimer("timer.combat_freeze")
        target.timer("timer.combat_freeze_immunity", FREEZE_IMMUNITY_TICKS)
    }

    public fun clearFreezeImmunity(target: Player) {
        target.freezeImmune = false
    }

    public fun statDrain(target: Player, stats: List<String>, amount: Int) {
        for (stat in stats) {
            target.statSub(stat, constant = amount, percent = 0)
        }
    }
}

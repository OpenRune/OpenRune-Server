package org.rsmod.api.combat.commons

import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statSub
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public object CombatEffects {

    public fun poison(target: Player, damage: Int) {
        PlayerPoison.tryPoison(target, initialDamage = damage)
    }

    private const val FREEZE_IMMUNITY_TICKS = 5

    public fun freeze(target: Player, ticks: Int): Boolean {
        if (target.isFrozen) return false
        if (target.freezeImmune) return false
        target.frozen = true
        target.routeDestination.clear()
        target.timer("timer.combat_freeze", ticks)
        target.mes("You have been frozen!", ChatType.Spam)
        return true
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

    public fun isFrozen(target: Npc): Boolean =
        target.vars["varn.freeze_end_clock"] > target.currentMapClock

    public fun freeze(target: Npc, ticks: Int, ignoreImmunity: Boolean = false): Boolean {
        val clock = target.currentMapClock
        if (!ignoreImmunity && target.vars["varn.freeze_immunity_end_clock"] > clock) return false
        val end = clock + ticks
        target.vars["varn.freeze_end_clock"] = end
        target.vars["varn.freeze_immunity_end_clock"] = end + FREEZE_IMMUNITY_TICKS
        target.routeDestination.clear()
        return true
    }

    public fun unfreeze(target: Npc) {
        val clock = target.currentMapClock
        target.vars["varn.freeze_end_clock"] = 0
        target.vars["varn.freeze_immunity_end_clock"] = clock + FREEZE_IMMUNITY_TICKS
    }

    public fun statDrain(target: Player, stats: List<String>, amount: Int) {
        for (stat in stats) {
            target.statSub(stat, constant = amount, percent = 0)
        }
    }
}

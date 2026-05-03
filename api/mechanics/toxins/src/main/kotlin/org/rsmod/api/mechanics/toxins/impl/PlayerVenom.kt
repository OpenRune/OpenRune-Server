package org.rsmod.api.mechanics.toxins.impl

import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.mechanics.toxins.Toxin
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

public object PlayerVenom {

    public const val TICK_INTERVAL: Int = PlayerPoison.TICK_INTERVAL

    public const val NOT_ENVENOMED: Int = 0

    public fun isEnvenomed(player: Player): Boolean = player.vars["varp.venom_strikes"] > NOT_ENVENOMED

    public fun damageForStrikeIndex(strikesCompletedSoFar: Int): Int =
        minOf(20, 6 + 2 * strikesCompletedSoFar.coerceAtLeast(0))

    public fun tryVenom(player: Player): Boolean = applyVenom(player)

    private fun applyVenom(player: Player): Boolean {
        if (PlayerPoison.hasWornPoisonEnvenomImmunity(player)) {
            return false
        }

        if (PlayerPoison.isPoisoned(player)) {
            PlayerPoison.clear(player)
        }

        VarPlayerIntMapSetter.set(player, "varp.venom_strikes", 1)
        player.timer("timer.player_venom", TICK_INTERVAL)
        player.mes("You have been envenomed!", ChatType.Spam)
        Toxin.syncStatusOrbs(player)
        return true
    }

    public fun clear(player: Player) {
        VarPlayerIntMapSetter.set(player, "varp.venom_strikes", NOT_ENVENOMED)
        player.clearTimer("timer.player_venom")
        Toxin.syncStatusOrbs(player)
    }

    public fun onVenomTimerTick(player: Player) {
        var strikes = player.vars["varp.venom_strikes"]
        if (strikes <= NOT_ENVENOMED) {
            clear(player)
            return
        }
        val strikeIndex = strikes - 1
        val damage = damageForStrikeIndex(strikeIndex)
        queueVenomHit(player, damage)
        strikes++
        VarPlayerIntMapSetter.set(player, "varp.venom_strikes", strikes)
        player.timer("timer.player_venom", TICK_INTERVAL)
        Toxin.syncStatusOrbs(player)
    }

    private fun queueVenomHit(player: Player, damage: Int) {
        player.queueHit(
            delay = 1,
            type = HitType.Typeless,
            damage = damage,
            hitmark = hitmark_groups.venom,
            modifier = NoopPlayerHitModifier,
        )
    }
}

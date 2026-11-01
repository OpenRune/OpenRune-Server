package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.damage.DamageContributor
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType

/**
 * Published once for every player who took part in a kill, after the npc's drops are handled.
 * Players who dealt damage are included even if they did not get the drop.
 *
 * Only kills that reach `NpcDeath.deathWithDrops` with a killer are seen, the same set that
 * `NpcDeathKillHook` covers.
 *
 * @param isHero this player is the one the kill's drops and kill count go to.
 * @param damage what this player dealt to the npc.
 * @param killingHit the hit that took the npc to zero, or null if something else did.
 * @param dealtKillingHit this player dealt [killingHit].
 * @param tookDamage the npc damaged this player at some point before dying.
 */
public class NpcKilledEvent(
    override val player: Player,
    public val npc: Npc,
    public val isHero: Boolean,
    public val damage: Int,
    public val killingHit: Hit?,
    public val dealtKillingHit: Boolean,
    public val tookDamage: Boolean,
) : UnboundEvent, PlayerEvent {
    /** The style of the killing hit, only for the player who dealt it. */
    public val killingStyle: HitType?
        get() = if (dealtKillingHit) killingHit?.type else null
}

/** The hero first, then everyone else who damaged [npc] and is still online. */
public fun npcKilledEvents(hero: Player, npc: Npc, players: PlayerList): List<NpcKilledEvent> {
    val damageDealt = LinkedHashMap<Player, Int>()
    damageDealt[hero] = npc.damageContributions.damageBy(hero)
    for (contributor in npc.damageContributions.entries()) {
        if (contributor is DamageContributor.ByPlayer) {
            val player = contributor.resolve(players) ?: continue
            damageDealt.putIfAbsent(player, contributor.damage)
        }
    }

    val killingHit = npc.killingHit
    val killer = killingHit?.resolvePlayerSource(players)
    return damageDealt.map { (player, damage) ->
        NpcKilledEvent(
            player = player,
            npc = npc,
            isHero = player === hero,
            damage = damage,
            killingHit = killingHit,
            dealtKillingHit = player === killer,
            tookDamage = npc.hasHurt(player),
        )
    }
}

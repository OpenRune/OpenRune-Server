package org.rsmod.api.player.hit.processor

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.game.damage.recordDamageOn
import org.rsmod.api.player.cheat.adminGodMode
import org.rsmod.api.player.death.recordDeathCause
import org.rsmod.api.player.death.resolveDeathCause
import org.rsmod.api.player.events.PlayerHitpointsChangedEvent
import org.rsmod.api.player.headbar.InternalPlayerHeadbars
import org.rsmod.api.player.queueDeath
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statSub
import org.rsmod.events.EventBus
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.Hit

public class DamageOnlyPlayerHitProcessor
@Inject
constructor(
    private val eventBus: EventBus,
    private val npcList: NpcList,
    private val playerList: PlayerList,
) : InstantPlayerHitProcessor {
    override fun Player.process(hit: Hit) {
        if (adminGodMode) {
            return
        }

        val oldHitpoints = hitpoints
        val damage = min(oldHitpoints, hit.damage)
        if (damage > 0) {
            statSub("stat.hitpoints", constant = damage, percent = 0)
            hit.recordDamageOn(this, damage, playerList, npcList)
            val event =
                PlayerHitpointsChangedEvent(
                    player = this,
                    oldHitpoints = oldHitpoints,
                    newHitpoints = hitpoints,
                    maxHitpoints = baseHitpointsLvl,
                    hit = hit,
                )
            eventBus.publish(event)
        }

        val queueDeath = hitpoints == 0 && "queue.death" !in queueList
        if (queueDeath) {
            val npcSource = if (hit.isFromNpc) hit.resolveNpcSource(npcList) else null
            val playerSource = if (hit.isFromPlayer) hit.resolvePlayerSource(playerList) else null
            recordDeathCause(hit.resolveDeathCause(npcSource, playerSource))
            queueDeath()
        }

        showHitmark(hit.hitmark)

        val headbar = hit.createHeadbar(hitpoints, baseHitpointsLvl)
        showHeadbar(headbar)
    }

    private fun Hit.createHeadbar(currHp: Int, maxHp: Int): Headbar =
        InternalPlayerHeadbars.createFromHitmark(hitmark, currHp, maxHp, "headbar.health_30")
}

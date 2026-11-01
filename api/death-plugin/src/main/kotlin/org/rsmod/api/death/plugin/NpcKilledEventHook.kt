package org.rsmod.api.death.plugin

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.events.npcKilledEvents
import org.rsmod.events.EventBus
import org.rsmod.game.entity.PlayerList

public class NpcKilledEventHook
@Inject
constructor(private val eventBus: EventBus, private val players: PlayerList) : NpcDeathKillHook {
    // Last, so kill counts and drops are already settled when subscribers run.
    override val priority: Int = -100

    override fun onKill(context: NpcDeathKillContext) {
        for (event in npcKilledEvents(context.hero, context.npc, players)) {
            eventBus.publish(event)
        }
    }
}

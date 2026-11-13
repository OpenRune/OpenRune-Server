package org.rsmod.api.game.process.player

import dev.openrune.types.StatType
import org.rsmod.api.player.events.PlayerAreaEvents
import org.rsmod.api.player.events.PlayerLevelEvents
import org.rsmod.api.player.stat.statBase
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.EngineQueueList
import org.rsmod.game.queue.EngineQueueType
import org.rsmod.game.stat.StatAdvance

internal fun EventBus.observeEngineQueue(player: Player, queue: EngineQueueList.Queue) {
    when (queue.type) {
        EngineQueueType.AdvanceStat.id -> publishIfHeard<PlayerLevelEvents.Advanced> {
            val advance = queue.args as StatAdvance
            val level = player.statBase(advance.stat.internalName)
            PlayerLevelEvents.Advanced(player, advance.stat, advance.previousLevel, level)
        }
        EngineQueueType.AdvanceCombat.id -> publishIfHeard<PlayerLevelEvents.CombatAdvanced> {
            PlayerLevelEvents.CombatAdvanced(player, queue.args as Int)
        }
        EngineQueueType.ChangeStat.id -> publishIfHeard<PlayerLevelEvents.StatChanged> {
            PlayerLevelEvents.StatChanged(player, queue.args as StatType)
        }
        EngineQueueType.Area.id -> publishIfHeard<PlayerAreaEvents.Entered> {
            PlayerAreaEvents.Entered(player, queue.label)
        }
        EngineQueueType.AreaExit.id -> publishIfHeard<PlayerAreaEvents.Exited> {
            PlayerAreaEvents.Exited(player, queue.label)
        }
    }
}

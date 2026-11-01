package org.rsmod.content.minigames.giantsfoundry

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathPreDropHook
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

class GiantsFoundryModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathPreDropHook>(FoundryDeathPreDropHook::class.java)
    }
}

class FoundryDeathPreDropHook
@Inject
constructor(private val eventBus: EventBus) : PlayerDeathPreDropHook {
    override fun beforeDrops(player: Player) {
        player.stashCarriedPreform(eventBus)
    }
}

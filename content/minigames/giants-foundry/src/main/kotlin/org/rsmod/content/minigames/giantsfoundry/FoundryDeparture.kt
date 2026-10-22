package org.rsmod.content.minigames.giantsfoundry

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

class GiantsFoundryModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathCleanupHook>(FoundryDeathCleanupHook::class.java)
        addSetBinding<PlayerTeleportValidateHook>(FoundryTeleportHook::class.java)
    }
}

class FoundryDeathCleanupHook
@Inject
constructor(private val eventBus: EventBus) : PlayerDeathCleanupHook {
    override fun cleanup(player: Player) {
        player.stashCarriedPreform(eventBus)
    }
}

class FoundryTeleportHook
@Inject
constructor(private val eventBus: EventBus) : PlayerTeleportValidateHook {
    override fun validate(
        player: Player,
        type: TeleportType,
        areaChecker: AreaChecker,
    ): String? {
        player.stashCarriedPreform(eventBus)
        return null
    }
}

package org.rsmod.content.interfaces.gameframe.script

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private object FriendsInterface {
    const val main: String = "interface.friends"
}

private object FriendsComponents {
    const val ignore: String = "component.friends:ignore"
}

private object IgnoreInterface {
    const val main: String = "interface.ignore"
}

private object IgnoreComponents {
    const val friends: String = "component.ignore:friends"
}

private object GameframeComponents {
    const val side9: String = "component.toplevel_osrs_stretch:side9"
}

class FriendsIgnoreScript
@Inject
constructor(
    private val eventBus: EventBus,
    private val playerList: PlayerList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOpen(FriendsInterface.main) {
            player.enableFriendsIgnoreButton()
        }

        onIfOpen(IgnoreInterface.main) {
            player.enableIgnoreFriendsButton()
        }

        onIfOverlayButton(FriendsComponents.ignore) {
            player.openIgnoreTab()
        }

        onIfOverlayButton(IgnoreComponents.friends) {
            player.openFriendsTab()
        }
    }

    private fun Player.enableFriendsIgnoreButton() {
        ifSetEvents(FriendsComponents.ignore, -1..-1, IfEvent.Op1)
        ifSetEvents(FriendsComponents.ignore, 0..0, IfEvent.Op1)
    }

    private fun Player.enableIgnoreFriendsButton() {
        ifSetEvents(IgnoreComponents.friends, -1..-1, IfEvent.Op1)
        ifSetEvents(IgnoreComponents.friends, 0..0, IfEvent.Op1)
    }

    private fun Player.openIgnoreTab() {
        ifOpenOverlay(IgnoreInterface.main, GameframeComponents.side9, eventBus)
    }

    private fun Player.openFriendsTab() {
        ifOpenOverlay(FriendsInterface.main, GameframeComponents.side9, eventBus)
    }
}
